package com.yuushya.modelling.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class ColorButton extends Button {
    public int color = Color.WHITE.getRGB();
    public boolean showEditor;

    public ColorButton(int x, int y, int width, int height, OnPress onPress, CreateNarration createNarration, int color) {
        super(x, y, width, height, Component.empty(), onPress, createNarration);
        this.color = color;
    }

    public ColorButton(int x, int y, int width, int height, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, Component.empty(), onPress, createNarration);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphicsExtractor, int i, int i1, float v) {
        if (showEditor) {
            guiGraphicsExtractor.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), Color.BLACK.getRGB());
        }
        guiGraphicsExtractor.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2, color | 0xFF000000);
    }
}
