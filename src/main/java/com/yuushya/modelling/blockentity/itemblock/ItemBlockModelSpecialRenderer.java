package com.yuushya.modelling.blockentity.itemblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.renderstate.ItemBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.SlotRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.rotate;
import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.scale;

public class ItemBlockModelSpecialRenderer implements SpecialModelRenderer<ItemBlockEntityRenderState> {
    public static final Identifier ITEM_BLOCK_MODEL_RENDERER = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "itemblock");
    public static final Identifier ITEM_BLOCK_MODEL_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/block/item_block.png");
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    /**
     * 绘制一个四边形面（逆时针顺序）
     */
    private static void renderQuad(
            PoseStack.Pose pose,
            VertexConsumer builder,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz
    ) {
        addVertex(pose, builder, x0, y0, z0, nx, ny, nz, 0.0F, 0.0F);
        addVertex(pose, builder, x1, y1, z1, nx, ny, nz, 1.0F, 0.0F);
        addVertex(pose, builder, x2, y2, z2, nx, ny, nz, 1.0F, 1.0F);
        addVertex(pose, builder, x3, y3, z3, nx, ny, nz, 0.0F, 1.0F);
    }

    /**
     * 绘制一个完整的正方体（边长为1格）
     */
    public static void renderCube(PoseStack.Pose pose, VertexConsumer builder) {
        float x0 = 0.0F, y0 = 0.0F, z0 = 0.0F;
        float x1 = 1.0F, y1 = 1.0F, z1 = 1.0F;

        // 北面 (z=0, 法线向北)
        renderQuad(pose, builder, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0F, 0.0F, -1.0F);
        // 南面 (z=1, 法线向南)
        renderQuad(pose, builder, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0F, 0.0F, 1.0F);
        // 西面 (x=0, 法线向西)
        renderQuad(pose, builder, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0F, 0.0F, 0.0F);
        // 东面 (x=1, 法线向东)
        renderQuad(pose, builder, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0F, 0.0F, 0.0F);
        // 顶面 (y=1, 法线向上)
        renderQuad(pose, builder, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0.0F, 1.0F, 0.0F);
    }

    /**
     * 添加一个顶点
     */
    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        builder.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Brightness.FULL_BRIGHT.pack())
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public void submit(@Nullable ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (state != null) {
            List<TransformItemData> transformData = state.transformData;
            List<SlotRenderState> renderData = state.renderData;
            if (transformData == null || transformData.isEmpty() || renderData == null || renderData.size() != transformData.size()) {
                return;
            }

            for (int i = 0; i < transformData.size(); i++) {
                TransformItemData transformDatum = transformData.get(i);
                if (!transformDatum.isShown || transformDatum.itemStack.isEmpty()) {
                    continue;
                }

                SlotRenderState slotRenderState = renderData.get(i);
                if (slotRenderState == null) {
                    continue;
                }

                poseStack.pushPose();

                poseStack.translate(0.5, 0.5, 0.5);
                scale(poseStack, transformDatum.scales);
                YuushyaUtils.translate(poseStack, transformDatum.pos);
                rotate(poseStack, transformDatum.rot);

                slotRenderState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);

                poseStack.popPose();
            }
        } else {
            submitNodeCollector.submitCustomGeometry(poseStack,
                    RenderTypes.itemCutout(ITEM_BLOCK_MODEL_TEXTURE),
                    (ItemBlockModelSpecialRenderer::renderCube));
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(10.0F, 10.0F, 10.0F));
        consumer.accept(new Vector3f(-10.0F, -10.0F, -10.0F));
    }

    @Override
    public @Nullable ItemBlockEntityRenderState extractArgument(ItemStack itemStack) {
        TypedEntityData<BlockEntityType<?>> blockEntityTypeTypedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityTypeTypedEntityData != null) {
            CompoundTag compoundTag = blockEntityTypeTypedEntityData.copyTagWithoutId();
            List<TransformItemData> transformDataList = new ArrayList<>();

            compoundTag.getList("transformData").ifPresent(listTag -> {
                for (Tag tag : listTag) {
                    TransformItemData.TRANSFORM_ITEM_DATA_CODEC.decode(NbtOps.INSTANCE, tag)
                            .resultOrPartial(error -> Yuushya.LOG_LOGGER.warn("Failed to decode ItemBlock transform data: {}", error))
                            .ifPresent(pair -> transformDataList.add(pair.getFirst()));
                }
            });

            if (transformDataList.isEmpty()) {
                return null;
            }

            ItemBlockEntityRenderState state = new ItemBlockEntityRenderState();
            state.transformData = transformDataList;

            // 为每个物品/方块解析 SlotRenderState
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel clientLevel = minecraft.level;
            ItemModelResolver itemModelResolver = minecraft.getItemModelResolver();
            BlockModelResolver blockModelResolver = minecraft.getBlockModelResolver();
            List<SlotRenderState> renderData = new ArrayList<>();

            for (TransformItemData transformDatum : transformDataList) {
                if (!transformDatum.isShown || transformDatum.itemStack.isEmpty()) {
                    renderData.add(null);
                    continue;
                }

                BlockState blockState = transformDatum.itemStack.get(DataComponentRegistry.BLOCKSTATE);
                if (transformDatum.enableBlock && blockState != null) {
                    BlockModelRenderState blockModelRenderState = new BlockModelRenderState();
                    blockModelResolver.update(blockModelRenderState, blockState, BLOCK_DISPLAY_CONTEXT);
                    renderData.add(SlotRenderState.ofBlock(blockModelRenderState));
                } else {
                    ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
                    itemModelResolver.updateForTopItem(itemStackRenderState, transformDatum.itemStack, ItemDisplayContext.NONE, clientLevel, null, 0);
                    renderData.add(SlotRenderState.ofItem(itemStackRenderState));
                }
            }
            state.renderData = new ArrayList<>(renderData);

            return state;
        }
        return null;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemBlockEntityRenderState> {
        public static final MapCodec<ItemBlockModelSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ItemBlockModelSpecialRenderer.Unbaked());

        @Override
        public @NonNull SpecialModelRenderer<ItemBlockEntityRenderState> bake(@NonNull BakingContext bakingContext) {
            return new ItemBlockModelSpecialRenderer();
        }

        @Override
        public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<ItemBlockEntityRenderState>> type() {
            return MAP_CODEC;
        }
    }
}
