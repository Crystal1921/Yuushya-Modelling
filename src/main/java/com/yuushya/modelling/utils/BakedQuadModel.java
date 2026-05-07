package com.yuushya.modelling.utils;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BakedQuadModel implements BlockStateModelPart {
    final List<BakedQuad> quads;
    final boolean useAmbientOcclusion;
    final Material.Baked particleMaterial;
    final int materialFlags;
    public BakedQuadModel(List<BakedQuad> quads, boolean useAmbientOcclusion, Material.Baked particleMaterial, int materialFlags) {
        this.quads = quads;
        this.useAmbientOcclusion = useAmbientOcclusion;
        this.particleMaterial = particleMaterial;
        this.materialFlags = materialFlags;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable Direction direction) {
        return this.quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.useAmbientOcclusion;
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.materialFlags;
    }

    public static @BakedQuad.MaterialFlags int computeMaterialFlags(List<BakedQuad> quads) {
        int flags = 0;

        for(BakedQuad quad : quads) {
            flags |= quad.materialInfo().flags();
        }

        return flags;
    }
}
