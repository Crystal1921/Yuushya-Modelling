package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

import java.awt.*;

public class ColorWidget extends AbstractWidget {
    private final ItemBlockScreen itemBlockScreen;
    float[] hsbvals = new float[3];

    public ColorWidget(int posX, int posY, int width, int height, int finalColor, Component message, ItemBlockScreen itemBlockScreen) {
        super(posX, posY, width, height, message);
        this.itemBlockScreen = itemBlockScreen;
        Color color = new Color(finalColor);
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbvals);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), FastColor.ARGB32.color(128, 255, 255, 255));
        RenderSystem.enableBlend();

        guiGraphics.blit(ColorTexture.getHueTextureLocation(), getX() + 5, getY() + 35, 0, 0, 0, 90, 20, 90, 20);
        PoseStack pose = guiGraphics.pose();

        this.renderSolidColor(pose, getX() + 5, getX() + 95, getY() + 65, getY() + 155, Color.HSBtoRGB(hsbvals[0], 1f, 1f));

        this.innerBlit(ColorTexture.getWhiteTextureLocation(), pose, getX() + 5, getX() + 95, getY() + 65, getY() + 155);
        this.innerBlit(ColorTexture.getBlackTextureLocation(), pose, getX() + 5, getX() + 95, getY() + 65, getY() + 155);

        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]));

        RenderSystem.disableBlend();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.itemBlockScreen.setFocused(this);
        this.itemBlockScreen.setDragging(true);

        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        if (mouseX >= 5 && mouseX <= 95 && mouseY >= 35 && mouseY <= 55) {
            float hue = (float) ((mouseX - 5) / 90.0);
            hsbvals[0] = hue;
            updateData();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (mouseX >= 5 && mouseX <= 95 && mouseY >= 65 && mouseY <= 155) {
            float brightness = 1.0f - (float) ((mouseY - 35) / 90.0);
            float saturation = (float) ((mouseX - 5) / 90.0);
            hsbvals[1] = saturation;
            hsbvals[2] = brightness;
            updateData();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(mouseX, mouseY, dragX, dragY);
        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        if (mouseX >= 5 && mouseX <= 95 && mouseY >= 35 && mouseY <= 55) {
            float hue = (float) ((mouseX - 5) / 90.0);
            hsbvals[0] = hue;
            updateData();
        }

        if (mouseX >= 5 && mouseX <= 95 && mouseY >= 65 && mouseY <= 155) {
            float brightness = 1.0f - (float) ((mouseY - 35) / 90.0);
            float saturation = (float) ((mouseX - 5) / 90.0);
            hsbvals[1] = saturation;
            hsbvals[2] = brightness;
            updateData();
        }
    }

    private void updateData() {
        itemBlockScreen.updateTransformData(ItemTransformType.COLOR, (double) (Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2])));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    void innerBlit(ResourceLocation atlasLocation, PoseStack poseStack, int x1, int x2, int y1, int y2) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) 0).setUv((float) 0.0, (float) 0.0);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, (float) 0).setUv((float) 0.0, (float) 1.0);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, (float) 0).setUv((float) 1.0, (float) 1.0);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, (float) 0).setUv((float) 1.0, (float) 0.0);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    void renderSolidColor(PoseStack poseStack, int x1, int x2, int y1, int y2, int color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader); // 使用颜色着色器

        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, 0).setColor(color);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, 0).setColor(color);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, 0).setColor(color);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, 0).setColor(color);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }
}
