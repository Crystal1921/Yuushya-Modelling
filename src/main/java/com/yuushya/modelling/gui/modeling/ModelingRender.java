package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 建模系列界面共用的叠加绘制工具：GL 状态、线段、盒与四边形。
 * <p>
 * 这里集中处理 {@code rendertype_lines} 的一个坑：它的顶点着色器把 {@code Normal} 当作
 * 「第二个顶点」来探测屏幕方向（{@code ProjMat * ModelViewMat * (Position + Normal)} 再与
 * {@code Position} 的投影相减，用 xy 差做线宽扩张）。GUI 的 {@code ProjMat} 是正交投影、
 * 只吃 xy，所以法线必须是<b>像素空间</b>的方向 —— 模型坐标的线段要先投影再取方向，
 * 否则沿 Z 的线会算出 {@code normalize(0)} 得到 NaN，整段被丢弃。
 * <p>
 * 视口里的辅助线（网格、线框、高亮）都用 {@link #beginOverlay()} / {@link #endOverlay()}
 * 包起来：关深度测试后它们压在最上层，不会与共面的体素面互相剔除。
 */
public final class ModelingRender {

    private ModelingRender() {
    }

    /** 叠加层统一的 GL 状态：混合开、剔除与深度测试关、不写深度。 */
    public static void beginOverlay() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    /** 还原 {@link #beginOverlay()} 改动的状态。 */
    public static void endOverlay() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** 端点已经是 GUI 像素坐标的线段：法线直接取像素方向。 */
    public static void segment(BufferBuilder builder, Matrix4f matrix,
                               float x0, float y0, float x1, float y1, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Math.max(1.0E-4F, (float) Math.hypot(dx, dy));
        float nx = dx / length;
        float ny = dy / length;
        builder.addVertex(matrix, x0, y0, 0.0F).setColor(color).setNormal(nx, ny, 0.0F);
        builder.addVertex(matrix, x1, y1, 0.0F).setColor(color).setNormal(nx, ny, 0.0F);
    }

    /** 模型坐标的线段：先把两端投影到像素空间求出方向，再按模型坐标提交。 */
    public static void line(BufferBuilder builder, Matrix4f matrix,
                            float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float[] dir = screenDirection(matrix, x0, y0, z0, x1, y1, z1);
        builder.addVertex(matrix, x0, y0, z0).setColor(color).setNormal(dir[0], dir[1], 0.0F);
        builder.addVertex(matrix, x1, y1, z1).setColor(color).setNormal(dir[0], dir[1], 0.0F);
    }

    /**
     * 线段在 GUI 像素空间的方向（单位向量）。
     * <p>
     * 线正好与视线平行、投影成一个点时退回水平方向，避免着色器里出现 {@code normalize(0, 0)}。
     */
    private static float[] screenDirection(Matrix4f matrix,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1) {
        Vector3f from = matrix.transformPosition(new Vector3f(x0, y0, z0));
        Vector3f to = matrix.transformPosition(new Vector3f(x1, y1, z1));
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return new float[]{1.0F, 0.0F};
        }
        return new float[]{dx / length, dy / length};
    }

    /** 轴对齐盒的 12 条棱。 */
    public static void box(BufferBuilder builder, Matrix4f matrix, AABB box, int color) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        line(builder, matrix, x0, y0, z0, x1, y0, z0, color);
        line(builder, matrix, x1, y0, z0, x1, y0, z1, color);
        line(builder, matrix, x1, y0, z1, x0, y0, z1, color);
        line(builder, matrix, x0, y0, z1, x0, y0, z0, color);
        line(builder, matrix, x0, y1, z0, x1, y1, z0, color);
        line(builder, matrix, x1, y1, z0, x1, y1, z1, color);
        line(builder, matrix, x1, y1, z1, x0, y1, z1, color);
        line(builder, matrix, x0, y1, z1, x0, y1, z0, color);
        line(builder, matrix, x0, y0, z0, x0, y1, z0, color);
        line(builder, matrix, x1, y0, z0, x1, y1, z0, color);
        line(builder, matrix, x1, y0, z1, x1, y1, z1, color);
        line(builder, matrix, x0, y0, z1, x0, y1, z1, color);
    }

    /** 提交一个顶点。 */
    public static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, int color) {
        builder.addVertex(matrix, x, y, z).setColor(color);
    }

    /** 提交一个四边形（四个顶点按顺序给）。 */
    public static void quad(BufferBuilder builder, Matrix4f matrix, int color,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3) {
        vertex(builder, matrix, x0, y0, z0, color);
        vertex(builder, matrix, x1, y1, z1, color);
        vertex(builder, matrix, x2, y2, z2, color);
        vertex(builder, matrix, x3, y3, z3, color);
    }
}