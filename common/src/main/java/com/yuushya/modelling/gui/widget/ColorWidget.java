package com.yuushya.modelling.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ColorWidget extends AbstractWidget {
    public ColorWidget(int posX, int posY, int width, int height, Component message) {
        super(posX, posY, width, height, message);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blit(ColorTexture.getLightTextureLocation(), getX(), getY(), 0, 0, 0, 90,16, 90,16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
