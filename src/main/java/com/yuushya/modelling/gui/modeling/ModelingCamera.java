package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 建模视口的相机状态，以及"方块局部坐标 → GUI 像素"的矩阵。
 * <p>
 * 渲染与 gizmo 投影共用同一份矩阵，避免"画在哪"和"点在哪"不一致。
 * <p>
 * GUI 阶段是正交投影，因此模型空间到屏幕是仿射映射：直接取矩阵变换后的 (x, y) 就是像素坐标，
 * 不需要透视除法。
 */
public final class ModelingCamera {

    /** 相机距离范围（以模型尺寸的倍数表示）。 */
    private static final float MIN_DISTANCE = 1.2F;
    private static final float MAX_DISTANCE = 24.0F;
    /** 模型占视口短边的比例上限。 */
    private static final float FIT_RATIO = 0.72F;

    private float yaw = 35.0F;
    private float pitch = 25.0F;
    private float distance = 3.5F;
    /** 每次相机变化 +1，供外部缓存（gizmo 的投影矩阵）判断是否需要重算。 */
    private int revision;

    public int revision() {
        return this.revision;
    }

    public void orbit(double deltaX, double deltaY) {
        this.yaw += (float) deltaX * 0.6F;
        this.pitch = (float) Mth.clamp(this.pitch + deltaY * 0.6F, -89.0F, 89.0F);
        this.revision++;
    }

    public void zoom(double notches) {
        this.distance = (float) Mth.clamp(this.distance * Math.pow(0.88, notches), MIN_DISTANCE, MAX_DISTANCE);
        this.revision++;
    }

    public void reset() {
        this.yaw = 35.0F;
        this.pitch = 25.0F;
        this.distance = 3.5F;
        this.revision++;
    }

    /** 我们自己的 pose：视口中心 + 适配缩放 + 俯仰 / 环绕。输出坐标即 GUI 像素。 */
    public Matrix4f baseMatrix(ModelingLayout.Rect rect) {
        float fit = Math.min(rect.width(), rect.height()) * FIT_RATIO / this.distance;
        return new Matrix4f()
                .translation(rect.x() + rect.width() / 2.0F, rect.y() + rect.height() / 2.0F, 150.0F)
                .scale(fit, -fit, fit)
                .rotateX((float) Math.toRadians(this.pitch))
                .rotateY((float) Math.toRadians(this.yaw));
    }

    /**
     * 叠加物品的显示变换（{@code FIXED}），得到与 {@code ItemRenderer.renderStatic} 完全一致的
     * "方块局部坐标 → GUI 像素"矩阵。
     * <p>
     * {@code ItemRenderer.render} 的链是：{@code 显示变换 → translate(-0.5,-0.5,-0.5)}，
     * 最后那一步把模型从 0..1 的方块坐标挪到以原点为中心。漏掉它会导致 gizmo 整体偏移
     * {@code 0.5 × fixedScale}（FIXED 缩放 0.5 时约 0.25 方块）。
     */
    public Matrix4f itemMatrix(ModelingLayout.Rect rect, ItemStack stack, BlockState fallbackState) {
        PoseStack pose = new PoseStack();
        pose.last().pose().set(baseMatrix(rect));

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = stack.isEmpty()
                ? minecraft.getBlockRenderer().getBlockModel(fallbackState)
                : minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
        ItemTransforms transforms = model.getTransforms();
        transforms.getTransform(ItemDisplayContext.FIXED).apply(false, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
        return new Matrix4f(pose.last().pose());
    }
}
