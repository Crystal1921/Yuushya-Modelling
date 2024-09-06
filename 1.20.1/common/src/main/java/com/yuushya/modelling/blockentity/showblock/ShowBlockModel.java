package com.yuushya.modelling.blockentity.showblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.world.level.block.ChestBlock;
import org.joml.Vector4f;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class ShowBlockModel implements BakedModel, UnbakedModel {
    protected final Direction facing;
    protected final BakedModel backup;
    public ShowBlockModel(Direction facing){
        this.facing = facing;
        this.backup = this;
    }
    public ShowBlockModel(Direction facing,BakedModel backup) {
        this.facing = facing;
        this.backup = backup;
    }
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, List<TransformData> transformDatas) {
        int vertexSize=YuushyaUtils.vertexSize();
        BlockRenderDispatcher blockRenderDispatcher =Minecraft.getInstance().getBlockRenderer();
        List<BakedQuad> finalQuads = new ArrayList<>();
        if (side != null) {return Collections.emptyList();}
        ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));directions.add(null); // 加个null
        float f = facing.toYRot();
        PoseStack stack = new PoseStack();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YP.rotationDegrees(-f));
        stack.translate(-0.5f, -0.5f, -0.5f);
        for(TransformData transformData:transformDatas)if (transformData.isShown){
            BlockState blockState = transformData.blockState;
            BakedModel blockModel = blockRenderDispatcher.getBlockModel(blockState);
            for (Direction value : directions) {
                List<BakedQuad> blockModelQuads = blockModel.getQuads(blockState, value, rand);
                for (BakedQuad bakedQuad : blockModelQuads) {
                    int[] vertex = bakedQuad.getVertices().clone();
                    // 执行核心方块的位移和旋转
                    stack.pushPose();{
                        YuushyaUtils.scale(stack, transformData.scales);
                        YuushyaUtils.translate(stack,transformData.pos);
                        YuushyaUtils.rotate(stack,transformData.rot);
                        for (int i = 0; i < 4; i++) {
                            Vector4f vector4f = new Vector4f(// 顶点的原坐标
                                    Float.intBitsToFloat(vertex[vertexSize*i]),
                                    Float.intBitsToFloat(vertex[vertexSize*i+1]),
                                    Float.intBitsToFloat(vertex[vertexSize*i+2]), 1);
                            stack.last().pose().transform(vector4f);
                            vertex[vertexSize*i] = Float.floatToRawIntBits(vector4f.x());
                            vertex[vertexSize*i+1] = Float.floatToRawIntBits(vector4f.y());
                            vertex[vertexSize*i+2] = Float.floatToRawIntBits(vector4f.z());
                        }
                    }stack.popPose();
                    if (bakedQuad.getTintIndex() > -1)//将方块状态和颜色编码到tintindex上，在渲染时解码找到对应颜色
                        finalQuads.add(new BakedQuad(vertex, YuushyaUtils.encodeTintWithState(bakedQuad.getTintIndex(), blockState), bakedQuad.getDirection(), bakedQuad.getSprite(), bakedQuad.isShade()));
                    else
                        finalQuads.add(new BakedQuad(vertex, bakedQuad.getTintIndex(), bakedQuad.getDirection(), bakedQuad.getSprite(), bakedQuad.isShade()));
                }
            }
        }
        return finalQuads;
    }



    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
        return backup.getQuads(blockState,side,rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        if(backup!=this){
            return backup.usesBlockLight();
        }
        return false;
    }

    @Override
    public boolean isGui3d() {
        if(backup!=this){
            return backup.isGui3d();
        }
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        if(backup!=this){
            return backup.usesBlockLight();
        }
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        if(backup!=this){
            return backup.isCustomRenderer();
        }
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if(backup!=this){
            return backup.getParticleIcon();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getParticleIcon();

    }

    @Override
    public ItemTransforms getTransforms() {
        if(backup!=this){
            return backup.getTransforms();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        if(backup!=this){
            return backup.getOverrides();
        }
        return ItemOverrides.EMPTY;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBaker modelBakery, Function<Material, TextureAtlasSprite> function, ModelState modelState, ResourceLocation resourceLocation) {return this;}

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {

    }
}
