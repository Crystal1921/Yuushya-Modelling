package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.awt.*;

public class ColorWidget extends AbstractWidget {
    /** 色板默认边长（110×160 的控件就是这个尺寸）。 */
    private static final int MAX_PALETTE = 75;
    private static final int MIN_PALETTE = 16;
    /** 色板下方留给 HSV 文本的高度。 */
    private static final int TEXT_SPACE = 16;
    /** 色相对控件顶部的位置：预览块 + 间距 + 色相条 + 间距。 */
    private static final int PALETTE_TOP = 65;

    private final ColorEditorHost colorScreen;
    /** 色板边长；面板比默认尺寸小时跟着缩小，整块色板才不会画到控件外面。 */
    private final int paletteSize;
    private final int xPadding = 5;
    private final int yPadding = 5;
    private final int yHeight = PALETTE_TOP;
    float[] hsbVals = new float[3];

    public ColorWidget(int posX, int posY, int width, int height, int finalColor, Component message, ColorEditorHost colorScreen) {
        super(posX, posY, width, height, message);
        this.colorScreen = colorScreen;
        int room = Math.min(width - 2 * xPadding, height - PALETTE_TOP - TEXT_SPACE);
        this.paletteSize = Math.clamp(room, MIN_PALETTE, MAX_PALETTE);
        Color color = new Color(finalColor);
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbVals);
    }

    /** 控件内容底边（含 HSV 文本行），用于画背景。 */
    private int contentBottom() {
        return Math.max(getHeight(), yHeight + paletteSize + TEXT_SPACE);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        String hsv = String.format("H: %.2f S: %.2f V: %.2f", hsbVals[0], hsbVals[1], hsbVals[2]);
        int bottom = contentBottom();
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + bottom + 1, FastColor.ARGB32.color(72, 0, 0, 0));
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + bottom, FastColor.ARGB32.color(72, 0, 0, 0));

        guiGraphics.drawString(colorScreen.getColorFont(), hsv, getX() + yPadding,
                getY() + yHeight + paletteSize + 5, FastColor.ARGB32.color(255, 255, 255, 255), false);
        RenderSystem.enableBlend();

        guiGraphics.blit(ColorTexture.getHueTextureLocation(), getX() + xPadding, getY() + 35, 0, 0, 0,
                paletteSize, 10, paletteSize, 10);
        PoseStack pose = guiGraphics.pose();

        this.renderSolidColor(pose, getX() + xPadding -1, getX() + xPadding + paletteSize + 1 , getY()+ yHeight -1, getY() + yHeight + paletteSize + 1, FastColor.ARGB32.color(72, 255, 255, 255));
        this.renderSolidColor(pose, getX() + xPadding, getX() + xPadding + paletteSize, getY() + yHeight, getY() + yHeight + paletteSize, Color.HSBtoRGB(hsbVals[0], 1f, 1f));

        this.innerBlit(ColorTexture.getWhiteTextureLocation(), pose, getX() + xPadding, getX() + xPadding + paletteSize, getY() + yHeight, getY() + yHeight + paletteSize);
        this.innerBlit(ColorTexture.getBlackTextureLocation(), pose, getX() + xPadding, getX() + xPadding + paletteSize, getY() + yHeight, getY() + yHeight + paletteSize);

        guiGraphics.fill(getX() + xPadding - 1, getY() + yPadding - 1, getX() + 25 + 1, getY() + 25 + 1, FastColor.ARGB32.color(72, 255, 255, 255));
        guiGraphics.fill(getX() + xPadding, getY() + yPadding, getX() + 25, getY() + 25, Color.HSBtoRGB(hsbVals[0], hsbVals[1], hsbVals[2]));

        RenderSystem.disableBlend();

        int hueX = (int) (getX() + xPadding + (hsbVals[0] * paletteSize));
        int hueY = getY() + 35;
        drawArrow(guiGraphics, hueX, hueY);

        int satX = (int) (getX() + xPadding + (hsbVals[1] * paletteSize));
        int valY = (int) (getY() + yHeight + ((1.0f - hsbVals[2]) * paletteSize));
        drawCross(guiGraphics, satX, valY);
    }

    private void drawCross(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.hLine(x - 2, x + 2 , y, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x, y - 3, y + 3, FastColor.ARGB32.color(255, 0, 0, 0));
    }

    private void drawArrow(GuiGraphics guiGraphics, int x, int y){
        guiGraphics.vLine(x, y - 4, y + 4, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x - 1, y - 4, y + 3, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x + 1, y - 4, y + 3, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x - 2, y - 4, y + 2, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x + 2, y - 4, y + 2, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x - 3, y - 4, y + 1, FastColor.ARGB32.color(255, 0, 0, 0));
        guiGraphics.vLine(x + 3, y - 4, y + 1, FastColor.ARGB32.color(255, 0, 0, 0));

        guiGraphics.vLine(x, y - 3, y + 3, FastColor.ARGB32.color(255, 255, 255, 255));
        guiGraphics.vLine(x - 1, y - 3, y + 2, FastColor.ARGB32.color(255, 255, 255, 255));
        guiGraphics.vLine(x + 1, y - 3, y + 2, FastColor.ARGB32.color(255, 255, 255, 255));
        guiGraphics.vLine(x - 2, y - 3, y + 1, FastColor.ARGB32.color(255, 255, 255, 255));
        guiGraphics.vLine(x + 2, y - 3, y + 1, FastColor.ARGB32.color(255, 255, 255, 255));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.colorScreen.setColorFocused(this);
        this.colorScreen.setColorDragging(true);

        changeColor(mouseX, mouseY);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(mouseX, mouseY, dragX, dragY);
        changeColor(mouseX, mouseY);
    }

    private void changeColor(double mouseX, double mouseY) {
        if (!this.visible) return;
        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        final int MARGIN = 4; // 允许超出的像素范围

        if (mouseY >= 35 && mouseY <= 45 && mouseX >= xPadding - MARGIN &&
                mouseX <= xPadding + paletteSize + MARGIN) {
            double clampedX = Math.max(xPadding, Math.min(mouseX, xPadding + paletteSize));
            float hue = (float) ((clampedX - xPadding) / paletteSize);
            hsbVals[0] = hue;
            updateData();
            setEditBox();
        }

        if (mouseY >= yHeight - MARGIN && mouseY <= yHeight + paletteSize + MARGIN &&
                mouseX >= xPadding - MARGIN && mouseX <= xPadding + paletteSize + MARGIN) {

            double clampedX = Math.max(xPadding, Math.min(mouseX, xPadding + paletteSize));
            double clampedY = Math.max(yHeight, Math.min(mouseY, yHeight + paletteSize));

            float brightness = 1.0f - (float) ((clampedY - yHeight) / paletteSize);
            float saturation = (float) ((clampedX - xPadding) / paletteSize);

            hsbVals[1] = saturation;
            hsbVals[2] = brightness;
            updateData();
            setEditBox();
        }
    }

    private void setEditBox() {
        int rgb = Color.getHSBColor(hsbVals[0], hsbVals[1], hsbVals[2]).getRGB();
        String hex = String.format("#%06X", (0xFFFFFF & rgb));
        colorScreen.getColorEditBox().setValue(hex);
    }

    private void updateData() {
        colorScreen.updateColorData(Color.HSBtoRGB(hsbVals[0], hsbVals[1], hsbVals[2]));
    }

    public int getColor() {
        return Color.HSBtoRGB(hsbVals[0], hsbVals[1], hsbVals[2]);
    }

    public void setColor(int color) {
        Color c = new Color(color);
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbVals);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

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
