package com.yuushya.modelling.blockentity.itemblock;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

import java.util.List;

public class ItemBlockModel implements BlockStateModel {
    @Override
    public void collectParts(RandomSource randomSource, List<BlockStateModelPart> list) {

    }

    @Override
    public Material.Baked particleMaterial() {
        return null;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return 0;
    }
//    @Getter
//    protected final Direction facing;
//    protected final BakedModel backup;
//
//    public ItemBlockModel(Direction facing) {
//        this.facing = facing;
//        this.backup = this;
//    }
//
//    public ItemBlockModel(Direction facing, BakedModel backup) {
//        this.facing = facing;
//        this.backup = backup;
//    }
//
//    @Override
//    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
//        if (backup != this) {
//            return backup.getQuads(blockState, side, rand);
//        }
//        return Collections.emptyList();
//    }
//
//    @Override
//    public boolean useAmbientOcclusion() {
//        if (backup != this) {
//            return backup.usesBlockLight();
//        }
//        return false;
//    }
//
//    @Override
//    public boolean isGui3d() {
//        if (backup != this) {
//            return backup.isGui3d();
//        }
//        return true;
//    }
//
//    @Override
//    public boolean usesBlockLight() {
//        if (backup != this) {
//            return backup.usesBlockLight();
//        }
//        return false;
//    }
//
//    @Override
//    public boolean isCustomRenderer() {
//        if (backup != this) {
//            return backup.isCustomRenderer();
//        }
//        return false;
//    }
//
//    @Override
//    public @NotNull TextureAtlasSprite getParticleIcon() {
//        if (backup != this) {
//            return backup.getParticleIcon();
//        }
//        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getParticleIcon();
//
//    }
//
//    @Override
//    public @NotNull ItemTransforms getTransforms() {
//        if (backup != this) {
//            return backup.getTransforms();
//        }
//        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getTransforms();
//    }
//
//    @Override
//    public @NotNull ItemOverrides getOverrides() {
//        if (backup != this) {
//            return backup.getOverrides();
//        }
//        return ItemOverrides.EMPTY;
//    }
//
//    @Override
//    public @NotNull Collection<Identifier> getDependencies() {
//        return Collections.emptyList();
//    }
//
//    @Nullable
//    @Override
//    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
//        return this;
//    }
//
//    @Override
//    public void resolveParents(Function<Identifier, UnbakedModel> function) {
//
//    }
}
