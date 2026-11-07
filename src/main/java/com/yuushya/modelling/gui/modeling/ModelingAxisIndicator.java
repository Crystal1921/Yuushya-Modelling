package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 视口右下角的导航轴（Blender 风格），用 OpenGL 直接绘制。
 * <p>
 * 六条轴端：正方向是实心圆点（点它 → 从该轴正方向看过去），负方向是小空心圆
 * （点它 → 从负方向看过去）。图形只依赖相机旋转，因此永远是当前视角的缩略示意。
 * <p>
 * 绘制分两批提交：轴线与圆环走 {@code rendertypeLines}（可用 {@code lineWidth} 控制粗细），
 * 实心端点走 {@code positionColor} 的三角形 —— 比用 {@code fill} 拼像素更干净，也没有锯齿。
 */
public final class ModelingAxisIndicator {

    /** 轴长（像素）。整体为原始尺寸的 2/3。 */
    private static final int AXIS_LENGTH = 17;
    /** 正方向端点半径。 */
    private static final int DOT_RADIUS = 5;
    /** 负方向端点半径。 */
    private static final int SMALL_DOT_RADIUS = 3;
    /** 与视口右/下边缘的间距。 */
    private static final int MARGIN = 7;

    private static final int[] COLORS = {0xFFE65A46, 0xFFA0DC5A, 0xFF5AB4DC};
    private static final int[] COLORS_HOVER = {0xFFFF8A70, 0xFFC6FF7A, 0xFF7AD4FF};
    private static final int AXIS_DIM = 0x66FFFFFF;

    private final ModelingCamera camera;
    private int hoverAxis = -1;
    private boolean hoverPositive;

    public ModelingAxisIndicator(ModelingCamera camera) {
        this.camera = camera;
    }

    private int centerX(ModelingLayout.Rect viewport) {
        return viewport.right() - MARGIN - AXIS_LENGTH - DOT_RADIUS;
    }

    private int centerY(ModelingLayout.Rect viewport) {
        return viewport.bottom() - MARGIN - AXIS_LENGTH - DOT_RADIUS;
    }

    private Vector3f rotate(int axis) {
        Matrix4f rotation = new Matrix4f()
                .rotateX((float) Math.toRadians(this.camera.pitch()))
                .rotateY((float) Math.toRadians(this.camera.yaw()));
        Vector3f direction = switch (axis) {
            case 0 -> new Vector3f(1.0F, 0.0F, 0.0F);
            case 1 -> new Vector3f(0.0F, 1.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 1.0F);
        };
        return rotation.transformDirection(direction);
    }

    /** 屏幕坐标：模型 +axis 指向屏幕右下方时 d.x 为正、d.y 取反（视口 Y 轴向下）。 */
    private int posX(ModelingLayout.Rect viewport, int axis) {
        return centerX(viewport) + Math.round(rotate(axis).x * AXIS_LENGTH);
    }

    private int posY(ModelingLayout.Rect viewport, int axis) {
        return centerY(viewport) - Math.round(rotate(axis).y * AXIS_LENGTH);
    }

    private int negX(ModelingLayout.Rect viewport, int axis) {
        return centerX(viewport) - Math.round(rotate(axis).x * AXIS_LENGTH);
    }

    private int negY(ModelingLayout.Rect viewport, int axis) {
        return centerY(viewport) + Math.round(rotate(axis).y * AXIS_LENGTH);
    }

    public void render(GuiGraphics guiGraphics, ModelingLayout.Rect viewport, int mouseX, int mouseY) {
        if (viewport.width() < 60 || viewport.height() < 60) {
            return;
        }
        updateHover(viewport, mouseX, mouseY);
        int cx = centerX(viewport);
        int cy = centerY(viewport);

        guiGraphics.flush();
        guiGraphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            drawGl(cx, cy, viewport);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void drawGl(int cx, int cy, ModelingLayout.Rect viewport) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f identity = new Matrix4f();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            // ---- 1. 线段：轴线 + 负方向的空心圆 ----
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(2.0F);
            BufferBuilder lines = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (int axis = 0; axis < 3; axis++) {
                int px = posX(viewport, axis);
                int py = posY(viewport, axis);
                int nx = negX(viewport, axis);
                int ny = negY(viewport, axis);
                addSegment(lines, identity, cx, cy, nx, ny, AXIS_DIM);
                boolean hovered = this.hoverAxis == axis && this.hoverPositive;
                addSegment(lines, identity, cx, cy, px, py, hovered ? COLORS_HOVER[axis] : COLORS[axis]);
            }
            for (int axis = 0; axis < 3; axis++) {
                boolean hovered = this.hoverAxis == axis && !this.hoverPositive;
                int radius = hovered ? SMALL_DOT_RADIUS + 1 : SMALL_DOT_RADIUS;
                addRing(lines, identity, negX(viewport, axis), negY(viewport, axis), radius,
                        hovered ? COLORS_HOVER[axis] : COLORS[axis]);
            }
            BufferUploader.drawWithShader(lines.buildOrThrow());

            // ---- 2. 实心圆：正方向端点 + 原点 ----
            // 用 TRIANGLES 逐三角形显式提交（TRIANGLE_FAN 在部分驱动/批处理路径下不可靠）
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder tris = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (int axis = 0; axis < 3; axis++) {
                boolean hovered = this.hoverAxis == axis && this.hoverPositive;
                int radius = hovered ? DOT_RADIUS + 1 : DOT_RADIUS;
                addDisc(tris, identity, posX(viewport, axis), posY(viewport, axis), radius,
                        hovered ? COLORS_HOVER[axis] : COLORS[axis]);
            }
            addDisc(tris, identity, cx, cy, 1, 0xFFFFFFFF);
            BufferUploader.drawWithShader(tris.buildOrThrow());
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void addSegment(BufferBuilder builder, Matrix4f matrix,
                                   float x0, float y0, float x1, float y1, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Math.max(1.0E-4F, (float) Math.hypot(dx, dy));
        float nx = dx / length;
        float ny = dy / length;
        builder.addVertex(matrix, x0, y0, 0.0F).setColor(color).setNormal(nx, ny, 0.0F);
        builder.addVertex(matrix, x1, y1, 0.0F).setColor(color).setNormal(nx, ny, 0.0F);
    }

    /** 圆环拆成 N 条线段（lines 模式无法直接画环）。 */
    private static void addRing(BufferBuilder builder, Matrix4f matrix, int cx, int cy, int radius, int color) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            addSegment(builder, matrix,
                    cx + (float) Math.cos(a0) * radius, cy + (float) Math.sin(a0) * radius,
                    cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius, color);
        }
    }

    /** 实心圆：拆成 24 个三角形（圆心 + 圆弧上相邻两点）。 */
    private static void addDisc(BufferBuilder builder, Matrix4f matrix, int cx, int cy, int radius, int color) {
        int segments = 24;
        float centerX = cx;
        float centerY = cy;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            builder.addVertex(matrix, centerX, centerY, 0.0F).setColor(color);
            builder.addVertex(matrix,
                    centerX + (float) Math.cos(a0) * radius,
                    centerY + (float) Math.sin(a0) * radius, 0.0F).setColor(color);
            builder.addVertex(matrix,
                    centerX + (float) Math.cos(a1) * radius,
                    centerY + (float) Math.sin(a1) * radius, 0.0F).setColor(color);
        }
    }

    // ---- 命中判定 ----

    private void updateHover(ModelingLayout.Rect viewport, int mouseX, int mouseY) {
        this.hoverAxis = -1;
        this.hoverPositive = false;
        if (!viewport.contains(mouseX, mouseY)) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (int axis = 0; axis < 3; axis++) {
            double dp = Math.hypot(mouseX - posX(viewport, axis), mouseY - posY(viewport, axis));
            if (dp <= DOT_RADIUS + 2 && dp < best) {
                best = dp;
                this.hoverAxis = axis;
                this.hoverPositive = true;
            }
            double dn = Math.hypot(mouseX - negX(viewport, axis), mouseY - negY(viewport, axis));
            if (dn <= SMALL_DOT_RADIUS + 2 && dn < best) {
                best = dn;
                this.hoverAxis = axis;
                this.hoverPositive = false;
            }
        }
    }

    /** @return 是否消费了本次点击 */
    public boolean mouseClicked(ModelingLayout.Rect viewport, double mouseX, double mouseY) {
        updateHover(viewport, (int) mouseX, (int) mouseY);
        if (this.hoverAxis < 0) {
            return false;
        }
        this.camera.lookFrom(this.hoverAxis, this.hoverPositive);
        return true;
    }
}
