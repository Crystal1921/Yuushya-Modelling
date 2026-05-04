package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.platform.NativeImage;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class ColorTexture implements AutoCloseable {
    @Getter
    private static Identifier HueTextureLocation;
    @Getter
    private static Identifier WhiteTextureLocation;
    @Getter
    private static Identifier BlackTextureLocation;

    private final DynamicTexture HueTexture;
    private final DynamicTexture WhiteTexture;
    private final DynamicTexture BlackTexture;

    public ColorTexture() {
        this.HueTexture = new DynamicTexture(256, 1, false);
        this.WhiteTexture = new DynamicTexture(256, 1, false);
        this.BlackTexture = new DynamicTexture(1, 256, false);
        HueTextureLocation = Minecraft.getInstance().getTextureManager().register("dynamic", this.HueTexture);
        WhiteTextureLocation = Minecraft.getInstance().getTextureManager().register("dynamic", this.WhiteTexture);
        BlackTextureLocation = Minecraft.getInstance().getTextureManager().register("dynamic", this.BlackTexture);

        NativeImage lightPixels = this.HueTexture.getPixels();
        NativeImage whitePixels = this.WhiteTexture.getPixels();
        NativeImage blackPixels = this.BlackTexture.getPixels();

        for (int x = 0; x < 256; x++) {
            // hue 范围 [0,1)，相当于色相环的 0°-360°
            float hue = x / 256.0f;
            int rgba = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

            int abgr = ShareUtils.getABGR(rgba);

            for (int y = 0; y < 1; y++) {
                lightPixels.setPixelRGBA(x, y, abgr);
            }
        }

        for (int x = 0; x < 256; x++) {
            // alpha 从 255 到 0
            int alpha = 255 - (x * 255 / 255);
            int r = 255, g = 255, b = 255;

            int rgba = (alpha << 24) | (r << 16) | (g << 8) | b;
            int abgr = ShareUtils.getABGR(rgba);

            for (int y = 0; y < 1; y++) {
                whitePixels.setPixelRGBA(x, y, abgr);
            }
        }

        for (int x = 0; x < 256; x++) {
            // alpha 从 255 到 0
            int alpha = (x * 255 / 255);
            int r = 0, g = 0, b = 0;

            int rgba = (alpha << 24) | (r << 16) | (g << 8) | b;
            int abgr = ShareUtils.getABGR(rgba);

            for (int y = 0; y < 1; y++) {
                blackPixels.setPixelRGBA(y, x, abgr);
            }
        }


        this.HueTexture.upload();
        this.WhiteTexture.upload();
        this.BlackTexture.upload();
    }

    @Override
    public void close() {
        this.HueTexture.close();
        this.WhiteTexture.close();
        this.BlackTexture.close();
    }
}
