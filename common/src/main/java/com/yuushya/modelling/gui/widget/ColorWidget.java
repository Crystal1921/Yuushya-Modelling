package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class ColorWidget extends AbstractWidget {
    private final ItemBlockScreen itemBlockScreen;
    @Setter
    private int color;

    public ColorWidget(int posX, int posY, int width, int height, int color, Component message, ItemBlockScreen itemBlockScreen) {
        super(posX, posY, width, height, message);
        this.itemBlockScreen = itemBlockScreen;
        this.color = color;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        RenderSystem.enableBlend();

        guiGraphics.blit(ColorTexture.getHueTextureLocation(), getX() + 5, getY() + 5, 0, 0, 0, 90, 20, 90, 20);
        PoseStack pose = guiGraphics.pose();

        this.renderSolidColor(pose, getX() + 5, getX() + 95, getY() + 35, getY() + 125, 0xFF000000 | color);

        this.innerBlit(ColorTexture.getWhiteTextureLocation(), pose, getX() + 5, getX() + 95, getY() + 35, getY() + 125
        );
        this.innerBlit(ColorTexture.getBlackTextureLocation(), pose, getX() + 5, getX() + 95, getY() + 35, getY() + 125
        );
        RenderSystem.disableBlend();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = mouseX - getX();
        mouseY = mouseY - getY();

        if (mouseX >= 5 && mouseX <= 95 && mouseY >= 5 && mouseY <= 25) {
            float hue = (float) ((mouseX - 5) / 90.0);
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f); // 饱和度=1，亮度=1
            this.color = rgb | 0xFF000000;
            itemBlockScreen.updateTransformData(ItemTransformType.COLOR, (double) (color));
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
