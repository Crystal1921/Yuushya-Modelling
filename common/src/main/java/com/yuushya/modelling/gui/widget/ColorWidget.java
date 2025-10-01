package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class ColorWidget extends AbstractWidget {
    private final ItemBlockScreen itemBlockScreen;
    private int color;
    public ColorWidget(int posX, int posY, int width, int height, Component message, ItemBlockScreen itemBlockScreen) {
        super(posX, posY, width, height, message);
        this.itemBlockScreen = itemBlockScreen;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.fill(this.getX(), this.getY() , this.getX() + 100, this.getY() + 30, Color.WHITE.getRGB());
        guiGraphics.blit(ColorTexture.getLightTextureLocation(), getX() + 5, getY() + 5, 0, 0, 0, 90, 20, 90, 20);
        guiGraphics.fill(this.getX(), this.getY() + 40 , this.getX() + 40, this.getY() + 80, 0xFF000000 | color);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        if (mouseX >= 5 &&mouseX <= 95 && mouseY >= 5 && mouseY <= 25) {
            float hue = (float) ((mouseX - 5) / 90.0);
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f); // 饱和度=1，亮度=1
            this.color = rgb | 0xFF000000;
            itemBlockScreen.updateTransformData(ItemTransformType.COLOR, (double)(color));
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
