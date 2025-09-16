package com.yuushya.modelling.blockentity.itemblock;

import net.minecraft.client.Minecraft;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ItemBlockModel implements BakedModel, UnbakedModel {
    protected final Direction facing;
    protected final BakedModel backup;

    public ItemBlockModel(Direction facing) {
        this.facing = facing;
        this.backup = this;
    }

    public ItemBlockModel(Direction facing, BakedModel backup) {
        this.facing = facing;
        this.backup = backup;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
        if (backup != this) {
            return backup.getQuads(blockState, side, rand);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        if (backup != this) {
            return backup.usesBlockLight();
        }
        return false;
    }

    @Override
    public boolean isGui3d() {
        if (backup != this) {
            return backup.isGui3d();
        }
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        if (backup != this) {
            return backup.usesBlockLight();
        }
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        if (backup != this) {
            return backup.isCustomRenderer();
        }
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (backup != this) {
            return backup.getParticleIcon();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getParticleIcon();

    }

    @Override
    public ItemTransforms getTransforms() {
        if (backup != this) {
            return backup.getTransforms();
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        if (backup != this) {
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
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        return this;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {

    }
}
