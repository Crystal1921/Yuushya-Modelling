package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.gui.modeling.target.ModelingChannel;
import com.yuushya.modelling.gui.modeling.target.ModelingTarget;
import com.yuushya.modelling.gui.transform.AxisDragController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/**
 * 视口里的坐标轴 gizmo：常显三条轴，点哪条就沿哪条拖。
 * <p>
 * 左键拖动 = 平移，右键拖动 = 绕该轴旋转。全部计算都在**方块局部坐标**（1 单位 = 1 方块）里做，
 * 再经 {@link ModelingCamera#itemMatrix} 投影到屏幕 —— 与模型渲染共用同一份矩阵，
 * 因此"画出来的线"与"能点中的线"必然一致。
 * <p>
 * 换算逻辑复用 {@link AxisDragController}：按下瞬间冻结轴的屏幕投影、过短的轴拒绝拖拽、
 * Shift 精细，这些行为与旧界面相同。
 */
public final class ModelingGizmo {

    private static final int[] COLORS = {0xFFE65A46, 0xFFA0DC5A, 0xFF5AB4DC};
    private static final int[] COLORS_HIGHLIGHT = {0xFFFF8A70, 0xFFC6FF7A, 0xFF7AD4FF};
    private static final float HANDLE_HALF_LENGTH = 0.55F;
    /** 拾取半径（像素）。太宽会误触，太窄难命中。 */
    private static final double PICK_RADIUS = 10.0;

    /** 参考网格：1 个方块宽，每边 8 等分。 */
    private static final int GRID_DIVISIONS = 8;
    /** 网格所在的底面高度（方块局部坐标的 y）。0 = 方块底面。 */
    private static final float GRID_PLANE_Y = 0.0F;
    private static final int GRID_LINE = 0x30FFFFFF;
    private static final int GRID_BORDER = 0x66FFFFFF;

    private static final ModelingChannel[] POS_CHANNELS = {
                ModelingChannel.POS_X, ModelingChannel.POS_Y, ModelingChannel.POS_Z};
        private static final ModelingChannel[] ROT_CHANNELS = {
                ModelingChannel.ROT_X, ModelingChannel.ROT_Y, ModelingChannel.ROT_Z};

        private final ModelingCamera camera;
        private final ModelingTarget target;
        private final IntSupplier slotSupplier;
        private final BiConsumer<ModelingChannel, Double> apply;
        private final AxisDragController controller;

        private ModelingLayout.Rect rect = new ModelingLayout.Rect(0, 0, 0, 0);
        private int highlighted;
        private Matrix4f matrixCache;
        private ModelingLayout.Rect matrixRect;
        private int matrixRevision = -1;

        public ModelingGizmo(ModelingCamera camera, ModelingTarget target,
                             IntSupplier slotSupplier, BiConsumer<ModelingChannel, Double> apply) {
            this.camera = camera;
            this.target = target;
            this.slotSupplier = slotSupplier;
            this.apply = apply;
            this.controller = new AxisDragController(new Host(), this::toNdc);
        }

    /** 每帧渲染前更新视口区域（相机矩阵依赖它）。 */
    public void setRect(ModelingLayout.Rect rect) {
        this.rect = rect;
    }

    /** 缓存"方块局部 → 像素"矩阵，避免每个采样点都重建一次；相机变化时失效。 */
        private Matrix4f matrix() {
                int revision = this.camera.revision();
                if (this.matrixCache == null || !this.rect.equals(this.matrixRect)
                        || revision != this.matrixRevision) {
                    this.matrixCache = this.camera.itemMatrix(this.rect);
                    this.matrixRect = this.rect;
                    this.matrixRevision = revision;
                }
                return this.matrixCache;
        }

    public boolean isActive() {
        return this.controller.isActive();
    }

    // ---- 交互 ----

    /** @return 是否开始了一次拖拽 */
    public boolean begin(double mouseX, double mouseY, boolean rotation, int screenWidth, int screenHeight) {
        int axis = this.controller.nearestAxis(mouseX, mouseY, screenWidth, screenHeight, PICK_RADIUS);
        if (axis < 0) {
            return false;
        }
        return this.controller.begin(axis, rotation, mouseX, mouseY, screenWidth, screenHeight);
    }

    public void update(double mouseX, double mouseY) {
        this.controller.update(mouseX, mouseY, net.minecraft.client.gui.screens.Screen.hasShiftDown());
    }

    public void end() {
        this.controller.end();
    }

    public void cancel() {
        this.controller.cancel();
    }

    /** 鼠标移动时高亮最近轴；返回是否命中。 */
    public boolean hover(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (this.controller.isActive()) {
            return true;
        }
        this.highlighted = this.controller.nearestAxis(mouseX, mouseY, screenWidth, screenHeight, PICK_RADIUS);
        return this.highlighted >= 0;
    }

    public void clearHover() {
        if (!this.controller.isActive()) {
            this.highlighted = -1;
        }
    }

    // ---- 渲染 ----

    /**
     * 参考网格：在<b>底面</b>（XZ 平面，y = {@link #GRID_PLANE_Y}）画一个 1 方块见方的网格，
     * 每边 {@link #GRID_DIVISIONS} 等分（每格 = 1/8 方块 = 方块纹理的 2 像素）。
     * <p>
     * 与模型共用同一份投影矩阵，因此无论怎么转视角都严格贴合方块；用 GL 线段绘制，
     * 半透明叠加，不会盖住模型细节。
     */
    public void renderGrid(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!hasData() || this.rect.width() <= 0) {
            return;
        }
        guiGraphics.flush();
        guiGraphics.enableScissor(this.rect.x(), this.rect.y(), this.rect.right(), this.rect.bottom());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        Matrix4f identity = new Matrix4f();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(1.0F);
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            float y = GRID_PLANE_Y;
            for (int i = 0; i <= GRID_DIVISIONS; i++) {
                float t = (float) i / GRID_DIVISIONS;
                int color = (i == 0 || i == GRID_DIVISIONS) ? GRID_BORDER : GRID_LINE;
                // 沿 Z 方向的线（固定 x = t）
                addGridSegment(builder, identity, t, y, 0.0F, t, y, 1.0F, color, screenWidth, screenHeight);
                // 沿 X 方向的线（固定 z = t）
                addGridSegment(builder, identity, 0.0F, y, t, 1.0F, y, t, color, screenWidth, screenHeight);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            guiGraphics.disableScissor();
        }
    }

    /** 网格线段：两端都在底面上，投影到屏幕后提交。 */
    private void addGridSegment(BufferBuilder builder, Matrix4f identity,
                                float x0, float y0, float z0, float x1, float y1, float z1, int color,
                                int screenWidth, int screenHeight) {
        float[] from = toScreen(new float[]{x0, y0, z0}, null, 0.0F, screenWidth, screenHeight);
        float[] to = toScreen(new float[]{x1, y1, z1}, null, 0.0F, screenWidth, screenHeight);
        if (from == null || to == null) {
            return;
        }
        addSegment(builder, identity, from[0], from[1], to[0], to[1], color);
    }

    /** 提交一条线段（顶点已是 GUI 像素坐标）。 */
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

    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        float[] origin = hostOrigin();
        if (origin == null) {
            return;
        }
        float[][] segments = new float[3][6];
        for (int axis = 0; axis < 3; axis++) {
            float[] direction = hostDirection(axis);
            float[] from = toScreen(origin, direction, -HANDLE_HALF_LENGTH, screenWidth, screenHeight);
            float[] to = toScreen(origin, direction, HANDLE_HALF_LENGTH, screenWidth, screenHeight);
            if (from == null || to == null) {
                segments[axis] = null;
                continue;
            }
            segments[axis] = new float[]{from[0], from[1], to[0], to[1], 0.0F, 0.0F};
        }
        float[] center = toScreen(origin, null, 0.0F, screenWidth, screenHeight);
        int active = this.controller.isActive() ? this.controller.axis() : this.highlighted;

        guiGraphics.enableScissor(this.rect.x(), this.rect.y(), this.rect.right(), this.rect.bottom());
        try {
            drawAxes(guiGraphics, segments, center, active);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /**
     * 用 OpenGL 线段绘制三条轴。
     * <p>
     * 之前的实现是用 {@code fill} 逐像素铺小方块，边缘锯齿、粗细也不均匀；这里直接走
     * {@code RenderType.lines} 的着色器，用 {@code lineWidth} 控制粗细，端点与颜色都是矢量结果。
     * <p>
     * 顶点已经是 GUI 像素坐标（GUI 的正交投影直接接收像素空间），所以矩阵用单位阵；
     * 期间关闭深度测试，保证轴线画在模型之上。
     */
    private void drawAxes(GuiGraphics guiGraphics, float[][] segments, float[] center, int active) {
        guiGraphics.flush();

        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f identity = new Matrix4f();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            for (int axis = 0; axis < 3; axis++) {
                float[] s = segments[axis];
                if (s == null) {
                    continue;
                }
                boolean lit = axis == active;
                RenderSystem.lineWidth(lit ? 4.0F : 2.0F);
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                addSegment(builder, identity, s[0], s[1], s[2], s[3], lit ? COLORS_HIGHLIGHT[axis] : COLORS[axis]);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        // 原点：画一个小方块做视觉锚点
        if (center != null) {
            int x = Math.round(center[0]);
            int y = Math.round(center[1]);
            guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
        }
    }

    // ---- 几何 ----

    /**
     * 元素中心在方块局部坐标里的位置。
     * <p>
     * {@code ShowBlockModel} 的链是 {@code F · [T(.5)·S·T(-.5)] · T(pos/16) · [T(.5)·R·T(-.5)]}，
     * 矩阵从右往左作用，所以 {@code T(pos/16)} 会被 {@code S} 缩放：中心 = {@code F · (0.5 + S·(pos/16))}，
     * 即 {@code 0.5 + pos·scales/16}（旋转绕自身中心，不改变中心位置）。
     * <p>
     * <b>F 在这里是单位变换</b>：视口绘制的是物品形态，走的是 {@code NeoShowBlockModel.getRenderPasses}，
     * 那里固定用 {@code Direction.SOUTH}（{@code toYRot() == 0}），不读取方块的朝向。
     * 若这里再按 {@code HORIZONTAL_FACING} 旋转一次，东西朝向的方块会把偏移从 X 转到 Z。
     */
    private float[] hostOrigin() {
        if (!hasData()) {
            return null;
        }
            ITransformDataProvider data = current();
            var pos = data.getPosition();
            var scales = data.getScale();
            return new float[]{
                    (float) (0.5 + pos.x * scales.x() / 16.0),
                    (float) (0.5 + pos.y * scales.y() / 16.0),
                    (float) (0.5 + pos.z * scales.z() / 16.0)};
        }

    private float[] hostDirection(int axis) {
        return switch (axis) {
            case 0 -> new float[]{1.0F, 0.0F, 0.0F};
            case 1 -> new float[]{0.0F, 1.0F, 0.0F};
            default -> new float[]{0.0F, 0.0F, 1.0F};
        };
    }

    /** 世界（这里是方块局部）坐标 → NDC。正交投影，直接取矩阵变换后的像素再换算。 */
    private Vector3f toNdc(float x, float y, float z) {
        int width = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float[] pixels = toScreen(new float[]{x, y, z}, null, 0.0F, width, height);
        if (pixels == null) {
            return null;
        }
        return new Vector3f(pixels[0] / width * 2.0F - 1.0F, 1.0F - pixels[1] / height * 2.0F, 0.0F);
    }

    private float[] toScreen(float[] origin, float[] direction, float offset, int screenWidth, int screenHeight) {
        float x = origin[0];
        float y = origin[1];
        float z = origin[2];
        if (direction != null) {
            x += direction[0] * offset;
            y += direction[1] * offset;
            z += direction[2] * offset;
        }
        if (!hasData() || this.rect.width() <= 0) {
            return null;
        }
        Vector4f point = matrix().transform(new Vector4f(x, y, z, 1.0F));
        return new float[]{point.x, point.y};
    }

    private boolean hasData() {
            return this.rect.width() > 0 && this.target.size() > 0
                    && this.slotSupplier.getAsInt() < this.target.size();
    }

        private ITransformDataProvider current() {
            return this.target.element(this.slotSupplier.getAsInt());
    }

    /** 供 {@link AxisDragController} 读写数据。 */
    private final class Host implements AxisDragController.Host {
        @Override
        public double value(int axis, boolean rotation) {
            if (!hasData()) {
                return 0.0;
            }
            // 旋转按"拖动切线轴上的位移"换算，缩放系数取 1（模型空间里 1 单位 = 1 方块）
                if (rotation) {
                    var rot = current().getRotation();
                    return switch (axis) {
                        case 0 -> rot.x();
                        case 1 -> rot.y();
                        default -> rot.z();
                    };
                }
                var pos = current().getPosition();
                return switch (axis) {
                    case 0 -> pos.x;
                    case 1 -> pos.y;
                    default -> pos.z;
                };
            }

            @Override
            public void apply(int axis, boolean rotation, double value) {
                ModelingGizmo.this.apply.accept((rotation ? ROT_CHANNELS : POS_CHANNELS)[axis], value);
            }

            @Override
            public void highlight(int axis) {
                ModelingGizmo.this.highlighted = axis;
            }

            @Override
            public double axisScale(int axis) {
                // 平移：pos 每 +1 让中心移动 scales/16，因此 1 个数据单位 = scales/16 方块
                // 旋转：绕该轴转 δ 时，切线上距中心 r 的点位移 r·δ（r 已被 S 缩放）
                if (!hasData()) {
                    return 1.0;
                }
                var scales = current().getScale();
                return switch (axis) {
                    case 0 -> scales.x();
                    case 1 -> scales.y();
                    default -> scales.z();
                };
            }

        @Override
        public float[] axisOriginWorld() {
            return hostOrigin();
        }

        @Override
        public float[] axisDirection(int axis) {
            return hostDirection(axis);
        }

        @Override
        public float handleHalfLength() {
            return HANDLE_HALF_LENGTH;
        }
    }
}
