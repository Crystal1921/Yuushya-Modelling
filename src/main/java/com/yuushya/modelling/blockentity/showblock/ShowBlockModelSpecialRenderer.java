package com.yuushya.modelling.blockentity.showblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.renderstate.ShowBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.registries.BlockRegistry;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShowBlockModelSpecialRenderer implements SpecialModelRenderer<ShowBlockEntityRenderState> {
    public static final Identifier SHOW_BLOCK_MODEL_RENDERER = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "showblock");
    public static final Identifier SHOW_BLOCK_MODEL_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/block/show_block.png");
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
    public void submit(@Nullable ShowBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (state != null) {
            List<TransformBlockData> transformData = state.transformData;
            List<BlockModelRenderState> blockModels = state.blockModelRenderStates;
            if (transformData == null || transformData.isEmpty() || blockModels == null || blockModels.size() != transformData.size()) {
                return;
            }

            for (int i = 0; i < transformData.size(); i++) {
                TransformBlockData transformDatum = transformData.get(i);
                if (!transformDatum.isShown) {
                    continue;
                }

                // 防止嵌套ShowBlock的无限递归渲染
                if (transformDatum.blockState.is(BlockRegistry.SHOW_BLOCK.get())) {
                    break;
                }

                BlockModelRenderState blockModel = blockModels.get(i);
                if (blockModel == null) {
                    continue;
                }

                poseStack.pushPose();

                YuushyaUtils.scale(poseStack, transformDatum.scales);
                YuushyaUtils.translate(poseStack, transformDatum.pos);
                YuushyaUtils.rotate(poseStack, transformDatum.rot);

                blockModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);

                poseStack.popPose();
            }
        } else {
            submitNodeCollector.submitCustomGeometry(poseStack,
                    RenderTypes.itemCutout(SHOW_BLOCK_MODEL_TEXTURE),
                    (ShowBlockModelSpecialRenderer::renderCube));
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(10.0F, 10.0F, 10.0F));
        consumer.accept(new Vector3f(-10.0F, -10.0F, -10.0F));
    }

    @Override
    public @Nullable ShowBlockEntityRenderState extractArgument(ItemStack itemStack) {
        TypedEntityData<BlockEntityType<?>> blockEntityTypeTypedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityTypeTypedEntityData != null) {
            CompoundTag compoundTag = blockEntityTypeTypedEntityData.copyTagWithoutId();
            List<TransformBlockData> transformDataList = new ArrayList<>();

            compoundTag.getList("transformData").ifPresent(listTag -> {
                for (Tag tag : listTag) {
                    TransformBlockData.TRANSFORM_BLOCK_DATA_CODEC.decode(NbtOps.INSTANCE, tag)
                            .resultOrPartial(error -> Yuushya.LOG_LOGGER.warn("Failed to decode ShowBlock transform data: {}", error))
                            .ifPresent(pair -> transformDataList.add(pair.getFirst()));
                }
            });

            if (transformDataList.isEmpty()) {
                return null;
            }

            ShowBlockEntityRenderState state = new ShowBlockEntityRenderState();
            state.transformData = transformDataList;

            // 为每个显示的方块解析 BlockModelRenderState
            Minecraft minecraft = Minecraft.getInstance();
            BlockModelResolver blockModelResolver = minecraft.getBlockModelResolver();
            List<BlockModelRenderState> blockModels = new ArrayList<>();

            for (TransformBlockData transformData : transformDataList) {
                BlockModelRenderState blockModelRenderState = new BlockModelRenderState();
                if (transformData.isShown && !transformData.blockState.is(BlockRegistry.SHOW_BLOCK.get())) {
                    blockModelResolver.update(blockModelRenderState, transformData.blockState, BLOCK_DISPLAY_CONTEXT);
                }
                blockModels.add(blockModelRenderState);
            }
            state.blockModelRenderStates = blockModels;

            return state;
        }
        return null;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ShowBlockEntityRenderState> {
        public static final MapCodec<ShowBlockModelSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ShowBlockModelSpecialRenderer.Unbaked());

        @Override
        public @NonNull SpecialModelRenderer<ShowBlockEntityRenderState> bake(@NonNull BakingContext bakingContext) {
            return new ShowBlockModelSpecialRenderer();
        }

        @Override
        public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<ShowBlockEntityRenderState>> type() {
            return MAP_CODEC;
        }
    }
}
