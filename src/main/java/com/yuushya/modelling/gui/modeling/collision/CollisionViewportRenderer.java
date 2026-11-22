package com.yuushya.modelling.gui.modeling.collision;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.gui.modeling.ModelingCamera;
import com.yuushya.modelling.gui.modeling.ModelingLayout;
import com.yuushya.modelling.gui.modeling.ModelingRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

/**
 * 碰撞箱雕刻视口的 3D 绘制：体素、三块画板、原碰撞箱参考线框、悬停预览。
 * <p>
 * 只负责画，不持有交互状态 —— 每帧由界面算好要画的范围再传进来。
 */
public final class CollisionViewportRenderer {

    /**
     * 一帧要画的内容。
     *
     * @param placeRange 左键放置的体素范围（闭区间 {@code {x0,y0,z0,x1,y1,z1}}），没有时为 {@code null}
     * @param eraseRange 右键挖除的体素范围，同上
     */
    public record State(boolean showGrid, boolean showOriginalShape, int[] placeRange, int[] eraseRange) {
    }

    /** 三块画板：0 = x=0（YZ），1 = y=0（XZ），2 = z=0（XY）。 */
    private static final int WALL_YZ = 0;
    private static final int WALL_XZ = 1;
    private static final int WALL_XY = 2;
    private static final int WALL_COUNT = 3;

    private static final int BACKGROUND = 0xFF151515;

    /** 体素面颜色（浅蓝灰），按方向乘一个亮度模拟光照。 */
    private static final int VOXEL_RED = 0xB6;
    private static final int VOXEL_GREEN = 0xC8;
    private static final int VOXEL_BLUE = 0xD6;
    private static final float[] FACE_SHADE = {0.55F, 1.00F, 0.82F, 0.76F, 0.70F, 0.86F};

    /** 三块画板的网格颜色（按所属轴着色：YZ 偏红、XZ 偏绿、XY 偏蓝）。 */
    private static final int[] WALL_LINE = {0x30FF9E90, 0x30A0DC5A, 0x305AB4DC};
    private static final int[] WALL_BORDER = {0x88FF9E90, 0x88A0DC5A, 0x885AB4DC};
    private static final int ORIGINAL_SHAPE_COLOR = 0x66FF9E5A;
    /** 方块 12 条棱的颜色，给贴在内侧的画板一个参照。 */
    private static final int CUBE_OUTLINE = 0x40FFFFFF;
    private static final int HOVER_PLACE = 0xFFFFFFFF;
    private static final int HOVER_PLACE_FILL = 0x40FFFFFF;
    private static final int HOVER_REMOVE = 0xFF5AD67D;
    private static final int HOVER_REMOVE_FILL = 0x405AD67D;

    private final CollisionVoxelGrid grid;
    private final AbstractTransformBlockEntity entity;
    private final ModelingCamera camera;

    public CollisionViewportRenderer(CollisionVoxelGrid grid,
                                     AbstractTransformBlockEntity entity,
                                     ModelingCamera camera) {
        this.grid = grid;
        this.entity = entity;
        this.camera = camera;
    }

    public void render(GuiGraphics guiGraphics, ModelingLayout.Rect rect, State state) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), BACKGROUND);
        guiGraphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
        try {
            // 体素在前：内部面已剔除，剩下的靠深度排序。线框随后画并关掉深度测试，
            // 否则画板格线 / 高亮框与体素面共面，深度相同会被零碎剔除（看起来像缺了一半）。
            renderVoxels(guiGraphics, rect);
            if (state.showGrid()) {
                renderWalls(guiGraphics, rect);
            }
            if (state.showOriginalShape()) {
                renderOriginalShape(guiGraphics, rect);
            }
            renderHover(guiGraphics, rect, state);
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /** 模型空间 → GUI 像素的矩阵：绘制与拾取共用一份，保证画的和点的是同一处。 */
    private Matrix4f viewMatrix(ModelingLayout.Rect rect) {
        return this.camera.baseMatrix(rect);
    }

    /**
     * 只画朝外的面：相邻格为空的六个方向，内部面直接跳过。
     * <p>
     * 自己走 {@code position_color} 而不是 {@code RenderType.debugQuads()}：后者的剔除状态不受控制，
     * 而相机把模型 Y 轴翻了过来（右手系变左手系），很容易把正面剔掉、只剩两三个背面。
     */
    private void renderVoxels(GuiGraphics guiGraphics, ModelingLayout.Rect rect) {
        if (this.grid.isEmpty()) {
            return;
        }
        Matrix4f matrix = viewMatrix(rect);
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int x = 0; x < CollisionVoxelGrid.SIZE; x++) {
                for (int y = 0; y < CollisionVoxelGrid.SIZE; y++) {
                    for (int z = 0; z < CollisionVoxelGrid.SIZE; z++) {
                        if (!this.grid.get(x, y, z)) {
                            continue;
                        }
                        for (Direction face : Direction.values()) {
                            if (!this.grid.get(x + face.getStepX(), y + face.getStepY(), z + face.getStepZ())) {
                                addFace(builder, matrix, x, y, z, face);
                            }
                        }
                    }
                }
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void addFace(BufferBuilder builder, Matrix4f matrix, int x, int y, int z, Direction face) {
        float x0 = x * (float) CollisionVoxelGrid.CELL;
        float y0 = y * (float) CollisionVoxelGrid.CELL;
        float z0 = z * (float) CollisionVoxelGrid.CELL;
        float x1 = x0 + (float) CollisionVoxelGrid.CELL;
        float y1 = y0 + (float) CollisionVoxelGrid.CELL;
        float z1 = z0 + (float) CollisionVoxelGrid.CELL;
        float shade = FACE_SHADE[face.ordinal()];
        int color = 0xFF000000
                | ((int) (VOXEL_RED * shade) << 16)
                | ((int) (VOXEL_GREEN * shade) << 8)
                | (int) (VOXEL_BLUE * shade);
        switch (face) {
            case DOWN -> {
                ModelingRender.vertex(builder, matrix, x0, y0, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y0, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y0, z1, color);
                ModelingRender.vertex(builder, matrix, x0, y0, z1, color);
            }
            case UP -> {
                ModelingRender.vertex(builder, matrix, x0, y1, z0, color);
                ModelingRender.vertex(builder, matrix, x0, y1, z1, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z1, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z0, color);
            }
            case NORTH -> {
                ModelingRender.vertex(builder, matrix, x0, y0, z0, color);
                ModelingRender.vertex(builder, matrix, x0, y1, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y0, z0, color);
            }
            case SOUTH -> {
                ModelingRender.vertex(builder, matrix, x0, y0, z1, color);
                ModelingRender.vertex(builder, matrix, x1, y0, z1, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z1, color);
                ModelingRender.vertex(builder, matrix, x0, y1, z1, color);
            }
            case WEST -> {
                ModelingRender.vertex(builder, matrix, x0, y0, z0, color);
                ModelingRender.vertex(builder, matrix, x0, y0, z1, color);
                ModelingRender.vertex(builder, matrix, x0, y1, z1, color);
                ModelingRender.vertex(builder, matrix, x0, y1, z0, color);
            }
            default -> {
                ModelingRender.vertex(builder, matrix, x1, y0, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z0, color);
                ModelingRender.vertex(builder, matrix, x1, y1, z1, color);
                ModelingRender.vertex(builder, matrix, x1, y0, z1, color);
            }
        }
    }

    /**
     * 三块画板：{@code x=0} / {@code y=0} / {@code z=0} 三个坐标平面各画 16×16 网格。
     * <p>
     * 它们就是方块的内侧三面（地板 + 左墙 + 后墙），默认视角看进去正好是个敞口盒子，
     * 起笔后笔刷沿法线朝你这边铺，方块内部就是可雕刻的空间。
     */
    private void renderWalls(GuiGraphics guiGraphics, ModelingLayout.Rect rect) {
        Matrix4f matrix = viewMatrix(rect);
        ModelingRender.overlayLines(guiGraphics, 1.0F, builder -> {
            for (int plane = 0; plane < WALL_COUNT; plane++) {
                for (int i = 0; i <= CollisionVoxelGrid.SIZE; i++) {
                    boolean border = i == 0 || i == CollisionVoxelGrid.SIZE;
                    addWallLines(builder, matrix, plane, i * (float) CollisionVoxelGrid.CELL,
                            border ? WALL_BORDER[plane] : WALL_LINE[plane]);
                }
            }
            // 方块本身的 12 条棱：画板都贴在内侧，没有这圈外框就看不出可雕刻的范围
            ModelingRender.box(builder, matrix, new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), CUBE_OUTLINE);
        });
    }

    /** 在某块画板上、距离边缘 {@code offset} 处画两条互相垂直的网格线。 */
    private static void addWallLines(BufferBuilder builder, Matrix4f matrix, int plane, float offset, int color) {
        switch (plane) {
            case WALL_YZ -> {
                ModelingRender.line(builder, matrix, 0.0F, offset, 0.0F, 0.0F, offset, 1.0F, color);
                ModelingRender.line(builder, matrix, 0.0F, 0.0F, offset, 0.0F, 1.0F, offset, color);
            }
            case WALL_XZ -> {
                ModelingRender.line(builder, matrix, offset, 0.0F, 0.0F, offset, 0.0F, 1.0F, color);
                ModelingRender.line(builder, matrix, 0.0F, 0.0F, offset, 1.0F, 0.0F, offset, color);
            }
            default -> {
                ModelingRender.line(builder, matrix, offset, 0.0F, 0.0F, offset, 1.0F, 0.0F, color);
                ModelingRender.line(builder, matrix, 0.0F, offset, 0.0F, 1.0F, offset, 0.0F, color);
            }
        }
    }

    /** 打开界面时的原碰撞箱，只做参考线框。 */
    private void renderOriginalShape(GuiGraphics guiGraphics, ModelingLayout.Rect rect) {
        VoxelShape shape = this.entity.getEffectiveShape();
        if (shape == null || shape.isEmpty()) {
            return;
        }
        Matrix4f matrix = viewMatrix(rect);
        ModelingRender.overlayLines(guiGraphics, 1.0F, builder -> {
            for (AABB box : shape.toAabbs()) {
                ModelingRender.box(builder, matrix, box, ORIGINAL_SHAPE_COLOR);
            }
        });
    }

    /**
     * 悬停高亮：白色 = 左键放置的实际体积（半透明填充 + 线框），绿色 = 右键挖除的实际体积。
     * <p>
     * 范围与点击落笔完全同源（都来自 {@link CollisionPicker} / 方框拖拽），画出来的就是会改的那一块。
     */
    private void renderHover(GuiGraphics guiGraphics, ModelingLayout.Rect rect, State state) {
        int[] placeRange = state.placeRange();
        int[] eraseRange = state.eraseRange();
        if (placeRange == null && eraseRange == null) {
            return;
        }
        Matrix4f matrix = viewMatrix(rect);
        int[] filled = placeRange != null ? placeRange : eraseRange;
        int fillColor = placeRange != null ? HOVER_PLACE_FILL : HOVER_REMOVE_FILL;
        ModelingRender.overlayQuads(guiGraphics, builder ->
                ModelingRender.boxFaces(builder, matrix, rangeBox(filled), fillColor));
        if (eraseRange != null) {
            ModelingRender.overlayLines(guiGraphics, 2.0F, builder ->
                    ModelingRender.box(builder, matrix, rangeBox(eraseRange), HOVER_REMOVE));
        }
        if (placeRange != null) {
            ModelingRender.overlayLines(guiGraphics, 2.0F, builder ->
                    ModelingRender.box(builder, matrix, rangeBox(placeRange), HOVER_PLACE));
        }
    }

    /** 体素范围（闭区间坐标）对应的方块局部空间盒子。 */
    private static AABB rangeBox(int[] range) {
        double cell = CollisionVoxelGrid.CELL;
        return new AABB(range[0] * cell, range[1] * cell, range[2] * cell,
                (range[3] + 1) * cell, (range[4] + 1) * cell, (range[5] + 1) * cell);
    }
}