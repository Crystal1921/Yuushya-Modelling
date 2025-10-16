package com.yuushya.modelling.neoforge.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;

import static com.yuushya.modelling.registries.YuushyaRegistries.COLOR_DATA;

public class PickColorOverlay implements LayeredDraw.Layer {
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel world = minecraft.level;
        Options options = minecraft.options;
        Player player = minecraft.player;
        if (player == null || world == null || options.hideGui) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        int guiWidth = guiGraphics.guiWidth() / 2;
        int guiHeight = guiGraphics.guiHeight() / 2;
        if (mainHandItem.is(YuushyaRegistries.ITEMS.get("color_picker_item"))) {

            Window window = Minecraft.getInstance().getWindow();
            int width = window.getScreenWidth();
            int height = window.getScreenHeight();
            // 计算屏幕中心坐标
            int centerX = width / 2;
            int centerY = height / 2;

            // 创建一个缓冲区用来存放像素数据（RGBA 各1字节）
            ByteBuffer buffer = BufferUtils.createByteBuffer(4);

            // 从当前帧缓冲读取中心像素颜色
            GL11.glReadPixels(centerX, centerY, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // 取出像素值（注意 glReadPixels 的原点在左下角）
            int r = (255 - buffer.get(0)) & 0xFF;
            int g = (255 - buffer.get(1)) & 0xFF;
            int b = (255 - buffer.get(2)) & 0xFF;
            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;

            guiGraphics.fill(guiWidth - 35, guiHeight - 35, guiWidth - 10, guiHeight - 10, Color.LIGHT_GRAY.getRGB());
            guiGraphics.fill(guiWidth - 33, guiHeight - 33, guiWidth - 12, guiHeight - 12, color);

            Integer i = mainHandItem.get((DataComponentType<Integer>) COLOR_DATA.get());
            if (i != null) {
                guiGraphics.fill(guiWidth + 10, guiHeight - 35, guiWidth + 35, guiHeight - 10, Color.LIGHT_GRAY.getRGB());
                guiGraphics.fill(guiWidth + 12, guiHeight - 33, guiWidth + 33, guiHeight - 12, i | 0xFF000000);
            }

        }
    }
}
