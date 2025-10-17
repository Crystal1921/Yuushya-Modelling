package com.yuushya.modelling.fabriclike.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockModel;
import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.CustomRenderInstance;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class FabricItemBlockModel extends ItemBlockModel implements UnbakedModel, BakedModel, FabricBakedModel {
    private static final Map<ItemStack, FabricItemBlockModel> itemModelCache = new ConcurrentHashMap<>();
    private static final long ITEM_RANDOM_SEED = 42L;
    public static int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
    public static int POSITION = findOffset(VertexFormatElement.POSITION);
    public static int COLOR = findOffset(VertexFormatElement.COLOR);
    public static int UV0 = findOffset(VertexFormatElement.UV0);
    public static int UV1 = findOffset(VertexFormatElement.UV1);
    public static int UV2 = findOffset(VertexFormatElement.UV2);
    public static int NORMAL = findOffset(VertexFormatElement.NORMAL);
    private final RandomSource random = RandomSource.create();
    private final Supplier<RandomSource> randomSupplier = () -> {
        random.setSeed(ITEM_RANDOM_SEED);
        return random;
    };


    public FabricItemBlockModel(Direction facing) {
        super(facing);
    }

    public FabricItemBlockModel(Direction facing, BakedModel backup) {
        super(facing, backup);
    }

    private static int findOffset(VertexFormatElement element) {
        if (DefaultVertexFormat.BLOCK.contains(element)) {
            // Divide by 4 because we want the int offset
            return DefaultVertexFormat.BLOCK.getOffset(element) / 4;
        }
        return -1;
    }

    public static int toABGR(int argb) {
        return (argb & 0xFF00FF00) // alpha and green same spot
                | ((argb >> 16) & 0x000000FF) // red moves to blue
                | ((argb << 16) & 0x00FF0000); // blue moves to red
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        ItemBlockEntity blockEntity = (ItemBlockEntity) blockView.getBlockEntity(pos);
        if (blockEntity == null) return;

        // 创建临时的 ItemBlockModel 用于处理块实体的渲染
        VanillaModelEncoder.emitBlockQuads(new FabricItemBlockModel(facing) {
            @Override
            public boolean isVanillaAdapter() {
                return true;
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return FabricItemBlockModel.this.getQuads(side, rand, blockEntity.getTransformData(), context, blockEntity.getBlockPos());
            }
        }, state, randomSupplier, context);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void emitItemQuads(ItemStack itemStack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        RegistryAccess registryAccess = level.registryAccess();

        if (data == CustomData.EMPTY) {
            // 使用备用模型进行渲染
            if (backup != null) {
                VanillaModelEncoder.emitItemQuads(backup, null, randomSupplier, context);
            }
            return;
        }

        // 获取缓存的模型并进行渲染
        FabricItemBlockModel cachedModel = itemModelCache.computeIfAbsent(itemStack, (_stack) -> new FabricItemBlockModel(Direction.SOUTH) {
            private final List<TransformItemData> transformDatas;

            {
                this.transformDatas = new ArrayList<>();
                ITransformItemDataInventory.load(data.copyTag(), transformDatas, registryAccess);
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return this.getQuads(side, rand, transformDatas, context, null);
            }
        });

        VanillaModelEncoder.emitItemQuads(cachedModel, null, randomSupplier, context);
    }

    public List<BakedQuad> getQuads(@Nullable Direction side, @NotNull RandomSource rand, List<TransformItemData> transformDatas, RenderContext context, @Nullable BlockPos pos) {
        int vertexSize = YuushyaUtils.vertexSize();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return Collections.emptyList();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
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

                if (pos != null) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    HashSet<BlockPos> orDefault = CustomRenderInstance.getINSTANCE().getCachedModeData().getOrDefault(chunkPos, new HashSet<>());
                    orDefault.add(pos);
                    CustomRenderInstance.getINSTANCE().getCachedModeData().put(chunkPos, orDefault);
                    CustomRenderInstance.getINSTANCE().dirty = true;
                    return Collections.emptyList();
                }
                if (!blockModel.isVanillaAdapter()) {
                    blockModel.emitItemQuads(itemStack, randomSupplier, context);
                }

                for (Direction value : directions) {
                    List<BakedQuad> blockModelQuads = blockModel.getQuads(null, value, rand);
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

                        final int fixedColor = toABGR(transformData.color);
                        for (int i = 0; i < 4; i++) vertex[i * STRIDE + COLOR] = fixedColor;

                        BakedQuad finalQuad = new BakedQuad(vertex, bakedQuad.getTintIndex(), bakedQuad.getDirection(), bakedQuad.getSprite(), bakedQuad.isShade());
                        finalQuads.add(finalQuad);
                    }
                }


            }
        return finalQuads;
    }
}