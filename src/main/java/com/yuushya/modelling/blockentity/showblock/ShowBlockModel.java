package com.yuushya.modelling.blockentity.showblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.utils.BakedQuadModel;
import com.yuushya.modelling.utils.YuushyaUtils;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowBlockModel implements BlockStateModel {
    @Override
    public void collectParts(final @NotNull RandomSource random, final @NotNull List<BlockStateModelPart> output) {
        //Noop
    }

    @Override
    public Material.@NonNull Baked particleMaterial() {
        return new Material.Baked(
                Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MissingTextureAtlasSprite.getLocation()),
                false
        );
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return 0;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (blockEntity instanceof ShowBlockEntity showBlockEntity) {
            List<TransformBlockData> transformDatas = showBlockEntity.getTransformData();
            BlockStateModelSet blockModelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
            int vertexSize = YuushyaUtils.vertexSize();

            ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
            directions.add(null); // 加个null

            float f = facing.toYRot();
            PoseStack stack = new PoseStack();
            stack.translate(0.5f, 0.5f, 0.5f);
            stack.mulPose(Axis.YP.rotationDegrees(-f));
            stack.translate(-0.5f, -0.5f, -0.5f);
            for (TransformBlockData transformData : transformDatas)
                if (transformData.isShown) {
                    BlockState blockState = transformData.blockState;
                    BlockStateModel blockStateModel = blockModelSet.get(blockState);
                    List<BlockStateModelPart> newParts = new ArrayList<>();
                    blockStateModel.collectParts(level, pos, blockState, random, newParts);
                    List<BakedQuad> blockModelQuads = new ArrayList<>();
                    List<BakedQuad> newQuads = new ArrayList<>();

                    newParts.forEach(modelPart -> directions.forEach(direction -> blockModelQuads.addAll(modelPart.getQuads(direction))));

                    for (BakedQuad bakedQuad : blockModelQuads) {
                        stack.pushPose();
                        YuushyaUtils.scale(stack, transformData.scales);
                        YuushyaUtils.translate(stack, transformData.pos);
                        YuushyaUtils.rotate(stack, transformData.rot);
                        Vector3fc[] vector4fs = new Vector3fc[4];
                        for (int i = 0; i < 4; i++) {
                            Vector3fc position = bakedQuad.position(i);
                            Vector4f vector4f = new Vector4f(position.x(), position.y(), position.z(), 1);
                            stack.last().pose().transform(vector4f);
                            vector4fs[i] = new Vector3f(vector4f.x(), vector4f.y(), vector4f.z());
                        }
                        stack.popPose();

                        newQuads.add(new BakedQuad(
                                vector4fs[0], vector4fs[1], vector4fs[2], vector4fs[3],
                                bakedQuad.packedUV0(), bakedQuad.packedUV1(), bakedQuad.packedUV2(), bakedQuad.packedUV3(),
                                bakedQuad.direction(), bakedQuad.materialInfo(), bakedQuad.bakedNormals(), bakedQuad.bakedColors()
                        ));
                    }

                    int materialFlags = BakedQuadModel.computeMaterialFlags(newQuads);
                    // TODO : 这里useAmbientOcclusion暂时写死了，后续可以考虑从原模型里获取
                    parts.add(new BakedQuadModel(newQuads, true, blockStateModel.particleMaterial(level, pos, state), materialFlags));
                }
        }
    }
}
