package com.yuushya.modelling.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class TextShader {
    public static Function<ResourceLocation, RenderType> TEXT = Util.memoize(TextShader::getText);

    private static RenderType getText(ResourceLocation locationIn) {
        var rendertype$state = RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
                .setTextureState(new CustomizableTextureState(locationIn, () -> NeoForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
                .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(RenderType.LIGHTMAP)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false);
        return RenderType.create("no_cull_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
    }

    private static class CustomizableTextureState extends RenderStateShard.TextureStateShard {
        private final BooleanSupplier blurSupplier;
        private final BooleanSupplier mipmapSupplier;

        private CustomizableTextureState(ResourceLocation resLoc, BooleanSupplier blur, BooleanSupplier mipmap) {
            super(resLoc, blur.getAsBoolean(), mipmap.getAsBoolean());
            blurSupplier = blur;
            mipmapSupplier = mipmap;
        }

        @Override
        public void setupRenderState() {
            // must be done before super call as super uses the `blur` and `mipmap` fields within the `setupState` runnable | See super constructor
            blur = blurSupplier.getAsBoolean();
            mipmap = mipmapSupplier.getAsBoolean();
            super.setupRenderState();
        }
    }
}
