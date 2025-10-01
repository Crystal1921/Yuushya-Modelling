package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class ColorTexture implements AutoCloseable {
    @Getter
    private static ResourceLocation lightTextureLocation;
    private final DynamicTexture lightTexture;

    public ColorTexture() {
        this.lightTexture = new DynamicTexture(256, 16, false); // 宽 256 比较平滑
        lightTextureLocation = Minecraft.getInstance().getTextureManager().register("dynamic", this.lightTexture);
        NativeImage lightPixels = this.lightTexture.getPixels();

        for (int x = 0; x < 256; x++) {
            // hue 范围 [0,1)，相当于色相环的 0°-360°
            float hue = x / 256.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

// 交换 R 和 B
            int a = (rgb >> 24) & 0xFF;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            int abgr = (a << 24) | (b << 16) | (g << 8) | r;

            for (int y = 0; y < 16; y++) {
                lightPixels.setPixelRGBA(x, y, abgr);
            }
        }

        this.lightTexture.upload();
    }

    @Override
    public void close() throws Exception {
        this.lightTexture.close();
    }
}
