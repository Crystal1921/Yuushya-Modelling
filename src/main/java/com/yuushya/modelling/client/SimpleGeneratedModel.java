package com.yuushya.modelling.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yuushya.modelling.Yuushya;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class SimpleGeneratedModel implements BakedModel {
    final List<BakedQuad>[] face = new List[6];
    final TextureAtlasSprite texture;
    final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "simple_generated");

    public SimpleGeneratedModel(TextureAtlasSprite texture) {
        this.face[0] = new ArrayList<>();
        this.face[1] = new ArrayList<>();
        this.face[2] = new ArrayList<>();
        this.face[3] = new ArrayList<>();
        this.face[4] = new ArrayList<>();
        this.face[5] = new ArrayList<>();
        this.texture = texture;
        float[] afloat = new float[]{0.0F, 0.0F, 16.0F, 16.0F};
        BlockFaceUV uv = new BlockFaceUV(afloat, 0);
        FaceBakery faceBakery = new FaceBakery();
        Vector3f to = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f from = new Vector3f(16.0F, 16.0F, 16.0F);
        BlockElementRotation bpr = null;
        BlockModelRotation mr = BlockModelRotation.X0_Y0;

        for(Direction side : Direction.values()) {
            BlockElementFace bpf = new BlockElementFace(side, 1, "", uv);
            Vector3f toB;
            Vector3f fromB;
            switch (side) {
                case UP:
                    toB = new Vector3f(to.x(), from.y(), to.z());
                    fromB = new Vector3f(from.x(), from.y(), from.z());
                    break;
                case EAST:
                    toB = new Vector3f(from.x(), to.y(), to.z());
                    fromB = new Vector3f(from.x(), from.y(), from.z());
                    break;
                case NORTH:
                    toB = new Vector3f(to.x(), to.y(), to.z());
                    fromB = new Vector3f(from.x(), from.y(), to.z());
                    break;
                case SOUTH:
                    toB = new Vector3f(to.x(), to.y(), from.z());
                    fromB = new Vector3f(from.x(), from.y(), from.z());
                    break;
                case DOWN:
                    toB = new Vector3f(to.x(), to.y(), to.z());
                    fromB = new Vector3f(from.x(), to.y(), from.z());
                    break;
                case WEST:
                    toB = new Vector3f(to.x(), to.y(), to.z());
                    fromB = new Vector3f(to.x(), from.y(), from.z());
                    break;
                default:
                    throw new NullPointerException();
            }

            BakedQuad g = faceBakery.bakeQuad(toB, fromB, bpf, texture, side, mr, bpr, false, textureLocation);
            this.face[side.ordinal()].add(g);
        }

    }

    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        return side == null ? Collections.emptyList() : this.face[side.ordinal()];
    }

    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return this.getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    public boolean useAmbientOcclusion() {
        return true;
    }

    public boolean isGui3d() {
        return true;
    }

    public boolean usesBlockLight() {
        return false;
    }

    public @NotNull TextureAtlasSprite getParticleIcon() {
        return this.texture;
    }

    public boolean isCustomRenderer() {
        return false;
    }

    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}