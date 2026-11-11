package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 建模界面的 3D 视口：实心背景 + 可自由环绕的相机。
 * <p>
 * 用「物品形态 + {@link net.minecraft.client.renderer.entity.ItemRenderer#renderStatic}」绘制：
 * 模型由 {@code NeoShowBlockModel} / {@code NeoItemBlockModel} 从 {@code BLOCK_ENTITY_DATA}
 * 重建，几何、渲染类型与光照都由物品渲染器处理。世界管线的 {@code renderSingleBlock}
 * 在 GUI 阶段没有配套状态，画不出来。
 * <p>
 * 这里只接收已经构建好的 {@link ItemStack}，具体是哪种方块实体由
 * {@link com.yuushya.modelling.gui.modeling.target.ModelingTarget#previewStack()} 决定。
 */
public final class ModelingViewport {

    public static final int BACKGROUND = 0xFF151515;

    private final ModelingCamera camera;

        public ModelingViewport(ModelingCamera camera) {
            this.camera = camera;
        }

        public void render(GuiGraphics guiGraphics, ModelingLayout.Rect rect, ItemStack itemStack) {
            if (rect.width() <= 0 || rect.height() <= 0) {
                return;
            }
            guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), BACKGROUND);

            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();

            guiGraphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
            try {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                // 与 ModelingCamera 共用同一份矩阵，gizmo 才能严丝合缝地对齐
                poseStack.mulPose(this.camera.baseMatrix(rect));

                minecraft.getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        poseStack, guiGraphics.bufferSource(), minecraft.level, 0);
                poseStack.popPose();
                guiGraphics.flush();
            } finally {
                guiGraphics.disableScissor();
            }
        }
    }
