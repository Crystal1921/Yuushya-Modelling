package com.yuushya.modelling.neoforge.client;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockModel;
import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static net.neoforged.neoforge.client.model.QuadTransformers.applyingColor;

public class NeoItemBlockModel extends ItemBlockModel implements IBakedModelExtension, BakedModel {
    public static final ModelResourceLocation TRIDENT_IN_HAND_MODEL;
    public static final ModelResourceLocation SPYGLASS_IN_HAND_MODEL;
    private static final Map<ItemStack, NeoItemBlockModel> itemModelCache = new HashMap<>();
    private static final ModelResourceLocation TRIDENT_MODEL;
    private static final ModelResourceLocation SPYGLASS_MODEL;
    public static ModelProperty<ItemBlockEntity> BASE_BLOCK_ENTITY = new ModelProperty<>();

    static {
        TRIDENT_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident"));
        TRIDENT_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident_in_hand"));
        SPYGLASS_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass"));
        SPYGLASS_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass_in_hand"));
    }

    public NeoItemBlockModel(Direction facing) {
        super(facing);
    }

    public NeoItemBlockModel(Direction facing, BakedModel backup) {
        super(facing, backup);
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) == null) {
            return ModelData.builder().build();
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBlockEntity blockEntity1)
                return ModelData.builder().with(BASE_BLOCK_ENTITY, blockEntity1).build();
            else
                return ModelData.builder().build();
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        ItemBlockEntity blockEntity = data.get(BASE_BLOCK_ENTITY);
        if (blockEntity == null) return Collections.emptyList();
        return this.getQuads(side, rand, blockEntity.getTransformData());
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Collections.emptyList();
        }
        RegistryAccess registryAccess = level.registryAccess();
        if (data == CustomData.EMPTY) {
            return List.of(backup);
        }
        return List.of(itemModelCache.computeIfAbsent(itemStack, (_stack) -> new NeoItemBlockModel(Direction.SOUTH) {
            private final List<TransformItemData> transformDatas;

            {
                this.transformDatas = new ArrayList<>();
                ITransformItemDataInventory.load(data.copyTag(), transformDatas, registryAccess);
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return this.getQuads(side, rand, transformDatas);
            }
        }));
    }

    public List<BakedQuad> getQuads(@Nullable Direction side, @NotNull RandomSource rand, List<TransformItemData> transformDatas) {
        int vertexSize = YuushyaUtils.vertexSize();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null) return Collections.emptyList();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        List<BakedQuad> finalQuads = new ArrayList<>();
        if (side != null) {
            return Collections.emptyList();
        }
        ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
        directions.add(null); // 加个null
        float f = facing.toYRot();
        PoseStack stack = new PoseStack();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YP.rotationDegrees(-f));
        stack.translate(-0.5f, -0.5f, -0.5f);
        for (TransformItemData transformData : transformDatas)
            if (transformData.isShown) {
                ItemStack itemStack = transformData.itemStack;
                BakedModel blockModel = itemRenderer.getModel(itemStack, null, null, player.getId());
                for (BakedModel model : blockModel.getRenderPasses(itemStack, true)) {
                    if (model instanceof BuiltInModel) {
                        model = itemRenderer.getItemModelShaper().getItemModel(itemStack);
                    }
                    for (Direction value : directions) {
                        List<BakedQuad> blockModelQuads = model.getQuads(null, value, rand);
                        for (BakedQuad bakedQuad : blockModelQuads) {
                            int[] vertex = bakedQuad.getVertices().clone();
                            // 执行核心方块的位移和旋转
                            stack.pushPose();
                            {
                                YuushyaUtils.scale(stack, transformData.scales);
                                YuushyaUtils.translate(stack, transformData.pos);
                                YuushyaUtils.rotate(stack, transformData.rot);
                                for (int i = 0; i < 4; i++) {
                                    Vector4f vector4f = new Vector4f(// 顶点的原坐标
                                            Float.intBitsToFloat(vertex[vertexSize * i]),
                                            Float.intBitsToFloat(vertex[vertexSize * i + 1]),
                                            Float.intBitsToFloat(vertex[vertexSize * i + 2]), 1);
                                    stack.last().pose().transform(vector4f);
                                    vertex[vertexSize * i] = Float.floatToRawIntBits(vector4f.x());
                                    vertex[vertexSize * i + 1] = Float.floatToRawIntBits(vector4f.y());
                                    vertex[vertexSize * i + 2] = Float.floatToRawIntBits(vector4f.z());
                                }
                            }
                            stack.popPose();
                            BakedQuad finalQuad = new BakedQuad(vertex, bakedQuad.getTintIndex(), bakedQuad.getDirection(), bakedQuad.getSprite(), bakedQuad.isShade());
                            applyingColor(Color.RED.getRGB()).processInPlace(finalQuad);
                            finalQuads.add(finalQuad);
                        }
                    }
                }
            }
        return finalQuads;
    }
}
