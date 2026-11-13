package com.yuushya.modelling.gui.modeling;

import net.minecraft.util.Mth;
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
    private static final float MIN_DISTANCE = 0.5F;
    private static final float MAX_DISTANCE = 24.0F;
    /** 模型占视口短边的比例上限。 */
    private static final float FIT_RATIO = 0.72F;

    private float yaw = 35.0F;
    private float pitch = 25.0F;
    private float distance = 3.5F;
    /** 视口内的平移偏移（GUI 像素），由 Shift + 拖动产生。 */
    private float panX;
    private float panY;
    /** 每次相机变化 +1，供外部缓存（gizmo 的投影矩阵）判断是否需要重算。 */
    private int revision;

    public int revision() {
        return this.revision;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
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

    /**
     * 平移视图（Shift + 拖动）。
     * <p>
     * 偏移量直接加在最外层平移上，因此是纯屏幕空间的移动：1 像素鼠标位移 = 1 像素画面位移，
     * 与当前缩放的远近无关（与 Blender 的 Shift + 中键一致）。
     */
    public void pan(double deltaX, double deltaY) {
        this.panX += (float) deltaX;
        this.panY += (float) deltaY;
        this.revision++;
    }

    public boolean isPanned() {
        return this.panX != 0.0F || this.panY != 0.0F;
    }

    public void reset() {
        this.yaw = 35.0F;
        this.pitch = 25.0F;
        this.distance = 3.5F;
        this.panX = 0.0F;
        this.panY = 0.0F;
        this.revision++;
    }

    /**
     * 把相机摆到"沿某根轴看过去"的视角（导航轴用）。
     * <p>
     * 推导：视图把模型旋转 {@code Rx(pitch)·Ry(yaw)}，所以想让模型的 {@code +axis} 朝向观察者时，
     * 该轴旋转后应指向 {@code +Z}。
     */
    public void lookFrom(int axis, boolean positive) {
        switch (axis) {
            case 0 -> {
                this.yaw = positive ? -90.0F : 90.0F;
                this.pitch = 0.0F;
            }
            case 1 -> {
                this.yaw = 0.0F;
                this.pitch = positive ? 90.0F : -90.0F;
            }
            default -> {
                this.yaw = positive ? 0.0F : 180.0F;
                this.pitch = 0.0F;
            }
        }
        this.revision++;
    }

    /**
         * 我们自己的 pose：视口中心 + 平移偏移 + 适配缩放 + 俯仰 / 环绕。输出坐标即 GUI 像素。
         * <p>
         * 末尾的 {@code translate(-0.5)} 把模型从 0..1 的方块坐标挪到以原点为中心 ——
         * 视口直接提交 {@code BakedQuad}，不再经过 {@code ItemRenderer} 的
         * {@code FIXED 显示变换 + translate(-0.5)} 那一套。
         */
        public Matrix4f baseMatrix(ModelingLayout.Rect rect) {
            float fit = Math.min(rect.width(), rect.height()) * FIT_RATIO / this.distance;
            return new Matrix4f()
                    .translation(rect.x() + rect.width() / 2.0F + this.panX,
                            rect.y() + rect.height() / 2.0F + this.panY, 150.0F)
                    .scale(fit, -fit, fit)
                    .rotateX((float) Math.toRadians(this.pitch))
                    .rotateY((float) Math.toRadians(this.yaw))
                    .translate(-0.5F, -0.5F, -0.5F);
        }

        /**
         * "方块局部坐标 → GUI 像素"矩阵。
         * <p>
         * 视口自己提交 {@code BakedQuad}，而 {@code NeoShowBlockModel} / {@code NeoItemBlockModel}
         * 已经把每个元素的位移、旋转、缩放烘焙进顶点，坐标仍是 0..1 的方块局部空间。
         * 因此这里与渲染共用 {@link #baseMatrix} 即可，不需要再叠显示变换。
         */
        public Matrix4f itemMatrix(ModelingLayout.Rect rect) {
            return baseMatrix(rect);
        }
    }
