package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.utils.DeprecatedMethod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ColorWidget extends AbstractWidget {
    private final AbstractColorScreen colorScreen;
    private final Identifier HUE_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/gui/hue_texture.png");
    private final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/gui/white_texture.png");
    private final Identifier BLACK_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/gui/black_texture.png");
    private final int WIDTH = 75;
    private final int xPadding = 5;
    private final int yPadding = 5;
    private final int yHeight = 65;
    float[] hsbVals = new float[3];

    public ColorWidget(int posX, int posY, int width, int height, int finalColor, Component message, AbstractColorScreen colorScreen) {
        super(posX, posY, width, height, message);
        this.colorScreen = colorScreen;
        Color color = new Color(finalColor);
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbVals);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tick) {
        String hsv = String.format("H: %.2f S: %.2f V: %.2f", hsbVals[0], hsbVals[1], hsbVals[2]);
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 26, ARGB.color(72, 0, 0, 0));
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight() + 25, ARGB.color(72, 0, 0, 0));

        guiGraphics.text(colorScreen.getColorFont(), hsv, getX() + yPadding, getY() + 145, ARGB.color(255, 255, 255, 255), false);

        DeprecatedMethod.blit(guiGraphics, HUE_TEXTURE, getX() + yPadding, getY() + 35, 0, 0, WIDTH, 10, WIDTH, 10);

        guiGraphics.fill(getX() + xPadding - 1, getY() + yHeight - 1, getX() + xPadding + WIDTH + 1, getY() + yHeight + WIDTH + 1, ARGB.color(72, 255, 255, 255));
        guiGraphics.fill(getX() + xPadding, getY() + yHeight, getX() + xPadding + WIDTH, getY() + yHeight + WIDTH, Color.HSBtoRGB(hsbVals[0], 1f, 1f));

        DeprecatedMethod.blit(guiGraphics, WHITE_TEXTURE, getX() + xPadding, getY() + yHeight, 0, 0, WIDTH, WIDTH, WIDTH, WIDTH);
        DeprecatedMethod.blit(guiGraphics, BLACK_TEXTURE, getX() + xPadding, getY() + yHeight, 0, 0, WIDTH, WIDTH, WIDTH, WIDTH);

        int hueX = (int) (getX() + xPadding + (hsbVals[0] * WIDTH));
        int hueY = getY() + 35;
        drawArrow(guiGraphics, hueX, hueY);

        int satX = (int) (getX() + xPadding + (hsbVals[1] * WIDTH));
        int valY = (int) (getY() + yHeight + ((1.0f - hsbVals[2]) * WIDTH));
        drawCross(guiGraphics, satX, valY);
    }

    private void drawCross(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.horizontalLine(x - 2, x + 2, y, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x, y - 3, y + 3, ARGB.color(255, 0, 0, 0));
    }

    private void drawArrow(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.verticalLine(x, y - 4, y + 4, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x - 1, y - 4, y + 3, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x + 1, y - 4, y + 3, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x - 2, y - 4, y + 2, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x + 2, y - 4, y + 2, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x - 3, y - 4, y + 1, ARGB.color(255, 0, 0, 0));
        guiGraphics.verticalLine(x + 3, y - 4, y + 1, ARGB.color(255, 0, 0, 0));

        guiGraphics.verticalLine(x, y - 3, y + 3, ARGB.color(255, 255, 255, 255));
        guiGraphics.verticalLine(x - 1, y - 3, y + 2, ARGB.color(255, 255, 255, 255));
        guiGraphics.verticalLine(x + 1, y - 3, y + 2, ARGB.color(255, 255, 255, 255));
        guiGraphics.verticalLine(x - 2, y - 3, y + 1, ARGB.color(255, 255, 255, 255));
        guiGraphics.verticalLine(x + 2, y - 3, y + 1, ARGB.color(255, 255, 255, 255));
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.colorScreen.setColorFocused(this);
        this.colorScreen.setColorDragging(true);

        changeColor(event.x(), event.y());

        return super.mouseClicked(event, doubleClick);
    }

    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        super.onDrag(event, dragX, dragY);
        changeColor(event.x(), event.y());
    }

    private void changeColor(double mouseX, double mouseY) {
        if (!this.visible) return;
        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        final int MARGIN = 4; // 允许超出的像素范围

        if (mouseY >= 35 && mouseY <= 45 && mouseX >= xPadding - MARGIN &&
                mouseX <= xPadding + WIDTH + MARGIN) {
            double clampedX = Math.clamp(mouseX, xPadding, xPadding + WIDTH);
            float hue = (float) ((clampedX - xPadding) / WIDTH);
            hsbVals[0] = hue;
            updateData();
            setEditBox();
        }

        if (mouseY >= yHeight - MARGIN && mouseY <= yHeight + WIDTH + MARGIN &&
                mouseX >= xPadding - MARGIN && mouseX <= xPadding + WIDTH + MARGIN) {

            double clampedX = Math.clamp(mouseX, xPadding, xPadding + WIDTH);
            double clampedY = Math.clamp(mouseY, yHeight, yHeight + WIDTH);

            float brightness = 1.0f - (float) ((clampedY - yHeight) / WIDTH);
            float saturation = (float) ((clampedX - xPadding) / WIDTH);

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

//    void innerBlit(Identifier atlasLocation, PoseStack poseStack, int x1, int x2, int y1, int y2) {
//        RenderSystem.setShaderTexture(0, atlasLocation);
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        Matrix4f matrix4f = poseStack.last().pose();
//        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) 0).setUv((float) 0.0, (float) 0.0);
//        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, (float) 0).setUv((float) 0.0, (float) 1.0);
//        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, (float) 0).setUv((float) 1.0, (float) 1.0);
//        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, (float) 0).setUv((float) 1.0, (float) 0.0);
//        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
//    }
//
//    void renderSolidColor(PoseStack poseStack, int x1, int x2, int y1, int y2, int color) {
//        RenderSystem.setShader(GameRenderer::getPositionColorShader); // 使用颜色着色器
//
//        Matrix4f matrix4f = poseStack.last().pose();
//        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, 0).setColor(color);
//        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, 0).setColor(color);
//        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, 0).setColor(color);
//        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, 0).setColor(color);
//        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
//    }
}
