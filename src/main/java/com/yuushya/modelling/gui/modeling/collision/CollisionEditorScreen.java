package com.yuushya.modelling.gui.modeling.collision;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.gui.modeling.ModelingAxisIndicator;
import com.yuushya.modelling.gui.modeling.ModelingCamera;
import com.yuushya.modelling.gui.modeling.ModelingChrome;
import com.yuushya.modelling.gui.modeling.ModelingLayout;
import com.yuushya.modelling.network.CustomShapePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 碰撞箱雕刻界面：在 3D 视口里把碰撞箱当成 16³ 体素来搭。
 * <p>
 * 三块「画板」就是方块局部空间的三个坐标平面 —— {@code x = 0}（YZ 面）、{@code y = 0}（XZ 面，地板）、
  * {@code z = 0}（XY 面，背墙），各画成 16×16 网格。在画板上点一格，就把当前笔刷形状从该面往方块内部放，
  * 于是从地板往上垒、从背墙往外补，像搭积木一样叠出碰撞箱。
 * <p>
 * 笔刷形状见 {@link BrushMode}：丁状（单格）、1/8、1/4、1/2 方块、条状、板状。
 * <p>
 * 交互（与 Minecraft 的放置 / 破坏一致）：
 * <ul>
 *   <li>左键：按当前笔刷放置；右键：按当前笔刷挖除</li>
 *   <li>左键拖动环绕视角、右键拖动平移、滚轮缩放；Ctrl+Z / Ctrl+Y 撤销 / 重做</li>
 * </ul>
 * 体素比画板更靠近相机（视线先穿过实体）时优先编辑体素，否则编辑画板。
 * <p>
 * 编辑的是一份 {@link CollisionVoxelGrid}，点「应用」时才通过 {@link CustomShapePacket} 写回服务端，
 * 并把方块形状设为 {@code CUSTOM}（否则新的碰撞箱不会参与判定）。
 */
public final class CollisionEditorScreen extends Screen {

    private static final int ROW = 20;
    private static final int TITLE_HEIGHT = ModelingLayout.PANEL_TITLE_HEIGHT;
    private static final int BUTTON_HEIGHT = 18;
    /** 按下后移动超过这个像素数就算拖动视角，不再触发放置 / 挖除。 */
    private static final double DRAG_THRESHOLD = 4.0;
    private static final int UNDO_LIMIT = 64;

    /** 三块画板：0 = x=0（YZ），1 = y=0（XZ），2 = z=0（XY）。 */
    private static final int WALL_YZ = 0;
    private static final int WALL_XZ = 1;
    private static final int WALL_XY = 2;
    private static final int WALL_COUNT = 3;

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

    private final Screen parent;
    private final AbstractTransformBlockEntity entity;
    private final CollisionVoxelGrid grid;
    private final Deque<long[]> undoStack = new ArrayDeque<>();
    private final Deque<long[]> redoStack = new ArrayDeque<>();

    private final ModelingCamera camera = new ModelingCamera();
    private final ModelingAxisIndicator axisIndicator = new ModelingAxisIndicator(this.camera);
    private final List<AbstractWidget> panels = new ArrayList<>();

    private ModelingLayout layout;
    private boolean showGrid = true;
    private boolean showOriginalShape = true;
    private Button undoButton;
    private Button redoButton;

    // 体素拾取结果
    private boolean voxelHover;
    private int hoverX;
    private int hoverY;
    private int hoverZ;
    /** 命中面的法线轴（0 = X，1 = Y，2 = Z），条状 / 板状靠它定向。 */
    private int hoverFaceAxis;
    /** 命中面外侧的格子（放置目标）；不放时 placeValid 为 false。 */
    private boolean placeValid;
    private int placeX;
    private int placeY;
    private int placeZ;

    // 画板拾取结果
    private int wallPlane = -1;
    private int wallCellU;
    private int wallCellV;

    /** 合并后的盒子数：重建 VoxelShape 有开销，只在数据变化后重算。 */
    private int cachedBoxCount;
    private boolean boxesDirty = true;

    /** 当前笔刷形状。 */
    private BrushMode brush = BrushMode.PIN;

    // 手势
    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_ORBIT = 1;
    private static final int GESTURE_PAN = 2;
    private int gesture = GESTURE_NONE;
    private int pressButton = -1;
    private double pressX;
    private double pressY;
    private boolean dragged;

    /**
     * 模拟「打开界面时摄像机已经转过 90°」。
     * <p>
     * 只借用现有的环绕接口推进初始 yaw，不改动 {@link ModelingCamera} 的任何行为；
     * 灵敏度与鼠标拖动环绕一致（0.6 度/像素），所以这里按像素量换算。
     */
    private static final double INITIAL_ORBIT_DEGREES = -90.0;
    private static final double ORBIT_DEGREES_PER_PIXEL = 0.6;

    public CollisionEditorScreen(Screen parent, AbstractTransformBlockEntity entity) {
        super(Component.translatable("gui.yuushya.collision.title"));
        this.parent = parent;
        this.entity = entity;
        this.grid = new CollisionVoxelGrid();
        this.grid.fromShape(entity.getCustomShape());
        this.camera.orbit(INITIAL_ORBIT_DEGREES / ORBIT_DEGREES_PER_PIXEL, 0.0);
    }

    /**
     * 笔刷形状：点一下要覆盖多大的体积。
     * <p>
     * 三个「方块比例」模式的边长按方块算（1/8 方块 = 2 格，1/4 = 4 格，1/2 = 8 格，居中展开）；
     * 条状沿当前平面 / 命中面的法线方向贯通整条；板状铺满除法线以外的两个方向。
     */
    public enum BrushMode {
        /** 单格（丁状）。 */
        PIN(1),
        /** 1/8 方块 = 2 格见方。 */
        EIGHTH(2),
        /** 1/4 方块 = 4 格见方。 */
        QUARTER(4),
        /** 1/2 方块 = 8 格见方。 */
        HALF(8),
        /** 沿主轴贯通的一根。 */
        BAR(0),
        /** 垂直于主轴铺满的一层。 */
        PLATE(0);

        private final int side;

        BrushMode(int side) {
            this.side = side;
        }

        public Component label() {
            return Component.translatable("gui.yuushya.collision.brush." + name().toLowerCase());
        }

        /**
         * 各轴的格数。
         *
         * @param primaryAxis 当前平面 / 命中面的法线轴
         */
        public int[] sizes(int primaryAxis) {
            int[] sizes = {this.side, this.side, this.side};
            switch (this) {
                case BAR -> sizes[primaryAxis] = CollisionVoxelGrid.SIZE;
                case PLATE -> {
                    sizes[0] = CollisionVoxelGrid.SIZE;
                    sizes[1] = CollisionVoxelGrid.SIZE;
                    sizes[2] = CollisionVoxelGrid.SIZE;
                    sizes[primaryAxis] = 1;
                }
                default -> {
                }
            }
            return sizes;
        }
    }

    // ---- 初始化 ----

    @Override
    protected void init() {
        this.layout = new ModelingLayout(this.width, this.height);
        rebuildPanels();
    }

    /** 左栏工具 + 右栏选项 + 顶栏应用/取消。 */
    private void rebuildPanels() {
        for (AbstractWidget widget : this.panels) {
            this.removeWidget(widget);
        }
        this.panels.clear();

        ModelingLayout.Rect left = this.layout.elements;
        int leftX = left.x() + 4;
        int leftWidth = Math.max(30, left.width() - 8);
        int y = left.y() + TITLE_HEIGHT + 2;
        addPanel(Button.builder(Component.translatable("gui.yuushya.collision.tool.fill"), btn -> fillAll())
                .bounds(leftX, y, leftWidth, ROW - 2).build());
        addPanel(Button.builder(Component.translatable("gui.yuushya.collision.tool.clear"), btn -> clearAll())
                .bounds(leftX, y + ROW, leftWidth, ROW - 2).build());
        this.undoButton = Button.builder(Component.translatable("gui.yuushya.collision.tool.undo"),
                btn -> undo()).bounds(leftX, y + ROW * 2, leftWidth, ROW - 2).build();
        addPanel(this.undoButton);
        this.redoButton = Button.builder(Component.translatable("gui.yuushya.collision.tool.redo"),
                btn -> redo()).bounds(leftX, y + ROW * 3, leftWidth, ROW - 2).build();
        addPanel(this.redoButton);
        addPanel(CycleButton.builder(BrushMode::label)
                .withValues(BrushMode.values())
                .withInitialValue(this.brush)
                .withTooltip(mode -> Tooltip.create(Component.translatable("gui.yuushya.collision.brush.tip")))
                .create(leftX, y + ROW * 4, leftWidth, ROW - 2,
                        Component.translatable("gui.yuushya.collision.brush"),
                        (button, mode) -> this.brush = mode));

        ModelingLayout.Rect right = this.layout.right;
        int rightX = right.x() + 4;
        int rightWidth = Math.max(40, right.width() - 8);
        int rightY = right.y() + TITLE_HEIGHT + 2;
        addPanel(Checkbox.builder(Component.translatable("gui.yuushya.collision.option.grid"), this.font)
                .pos(rightX, rightY).maxWidth(rightWidth).selected(this.showGrid)
                .onValueChange((box, value) -> this.showGrid = value)
                .build());
        addPanel(Checkbox.builder(Component.translatable("gui.yuushya.collision.option.original"), this.font)
                .pos(rightX, rightY + ROW).maxWidth(rightWidth).selected(this.showOriginalShape)
                .onValueChange((box, value) -> this.showOriginalShape = value)
                .build());

        ModelingLayout.Rect header = this.layout.header;
        int cancelWidth = 40;
        int applyWidth = 40;
        int headerY = header.y() + (header.height() - BUTTON_HEIGHT) / 2;
        addPanel(Button.builder(Component.translatable("gui.yuushya.collision.apply"), btn -> apply())
                .bounds(header.right() - 4 - cancelWidth - 4 - applyWidth, headerY, applyWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.yuushya.collision.apply.tip")))
                .build());
        addPanel(Button.builder(Component.translatable("gui.yuushya.collision.cancel"), btn -> onClose())
                .bounds(header.right() - 4 - cancelWidth, headerY, cancelWidth, BUTTON_HEIGHT)
                .build());

        refreshUndoButtons();
    }

    private void addPanel(AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.panels.add(widget);
    }

    private void refreshUndoButtons() {
        if (this.undoButton != null) {
            this.undoButton.active = !this.undoStack.isEmpty();
        }
        if (this.redoButton != null) {
            this.redoButton.active = !this.redoStack.isEmpty();
        }
    }

    // ---- 编辑动作 ----

    /** 改动前先压一份快照，撤销才有得回。 */
    private void pushUndo() {
        if (this.undoStack.size() >= UNDO_LIMIT) {
            this.undoStack.removeLast();
        }
        this.undoStack.push(this.grid.snapshot());
        this.redoStack.clear();
        refreshUndoButtons();
    }

    private void undo() {
        if (this.undoStack.isEmpty()) {
            return;
        }
        this.redoStack.push(this.grid.snapshot());
        this.grid.restore(this.undoStack.pop());
        this.boxesDirty = true;
        refreshUndoButtons();
    }

    private void redo() {
        if (this.redoStack.isEmpty()) {
            return;
        }
        this.undoStack.push(this.grid.snapshot());
        this.grid.restore(this.redoStack.pop());
        this.boxesDirty = true;
        refreshUndoButtons();
    }

    private void fillAll() {
        if (this.grid.count() == CollisionVoxelGrid.CELLS) {
            return;
        }
        pushUndo();
        this.grid.fillAll();
        this.boxesDirty = true;
    }

    private void clearAll() {
        if (this.grid.isEmpty()) {
            return;
        }
        pushUndo();
        this.grid.clear();
        this.boxesDirty = true;
    }

    /**
     * 从锚点出发、按当前笔刷算出的作用范围。
     *
     * @param anchor      锚点体素（画板格为贴平面那一格）
     * @param primaryAxis 主轴（画板法线 / 命中面法线），条状沿它贯通、板状垂直于它铺开
     * @return {@code {x0, y0, z0, x1, y1, z1}}（闭区间）
     */
    private int[] brushRange(int[] anchor, int primaryAxis) {
        int[] sizes = this.brush.sizes(primaryAxis);
        int[] min = new int[3];
        int[] max = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            int size = Math.min(CollisionVoxelGrid.SIZE, sizes[axis]);
            int start;
            if (size >= CollisionVoxelGrid.SIZE) {
                start = 0;
            } else if (size == 1) {
                start = anchor[axis];
            } else {
                // 边长大于 1 时让锚点落在中间，点哪儿就在哪儿展开
                start = anchor[axis] - (size - 1) / 2;
            }
            start = Math.clamp(start, 0, CollisionVoxelGrid.SIZE - size);
            min[axis] = start;
            max[axis] = start + size - 1;
        }
        return new int[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    /**
     * 当前鼠标位置的笔刷锚点与主轴。
     *
     * @param place 是否为放置（左键）；否则是挖除（右键）
     * @return {@code {x, y, z, primaryAxis}}；没有可编辑目标时返回 {@code null}
     */
    private int[] actionAnchor(boolean place) {
        if (this.wallPlane >= 0) {
            int[] cell = wallCellToVoxel(this.wallPlane, this.wallCellU, this.wallCellV);
            return new int[]{cell[0], cell[1], cell[2], wallAxis(this.wallPlane)};
        }
        if (!this.voxelHover) {
            return null;
        }
        if (place) {
            if (!this.placeValid) {
                return null;
            }
            return new int[]{this.placeX, this.placeY, this.placeZ, this.hoverFaceAxis};
        }
        return new int[]{this.hoverX, this.hoverY, this.hoverZ, this.hoverFaceAxis};
    }

    /** 鼠标位置会被改成目标状态的那块体积；用于高亮预览。 */
    private int[] previewRange() {
        int[] anchor = actionAnchor(true);
        if (anchor == null) {
            anchor = actionAnchor(false);
        }
        return anchor == null ? null : brushRange(anchor, anchor[3]);
    }

    /**
     * 按当前笔刷放置 / 挖除。
     * <p>
     * 先检查范围内是否真有格子需要改，避免"点一下没变化也压一份撤销"；整块一起改，撤销只算一步。
     */
    private void paintBrush(int[] anchor, boolean filled) {
        int[] range = brushRange(anchor, anchor[3]);
        boolean changed = false;
        for (int x = range[0]; x <= range[3] && !changed; x++) {
            for (int y = range[1]; y <= range[4] && !changed; y++) {
                for (int z = range[2]; z <= range[5] && !changed; z++) {
                    if (this.grid.get(x, y, z) != filled) {
                        changed = true;
                    }
                }
            }
        }
        if (!changed) {
            return;
        }
        pushUndo();
        for (int x = range[0]; x <= range[3]; x++) {
            for (int y = range[1]; y <= range[4]; y++) {
                for (int z = range[2]; z <= range[5]; z++) {
                    this.grid.set(x, y, z, filled);
                }
            }
        }
        this.boxesDirty = true;
    }

    private void apply() {
        CustomShapePacket.sendToServerSide(this.entity.getBlockPos(), this.grid.toShape(), true);
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 渲染 ----

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.layout == null) {
            this.layout = new ModelingLayout(this.width, this.height);
        }
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        updateHover(mouseX, mouseY);
        renderViewport(guiGraphics);
        this.axisIndicator.render(guiGraphics, this.layout.viewport, mouseX, mouseY);
        ModelingChrome.render(guiGraphics, this.font, this.layout, this.entity.getBlockState().getBlock().getName(),
                null, "gui.yuushya.collision.tools", "gui.yuushya.collision.options");
        renderCounters(guiGraphics);
        renderHint(guiGraphics);
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, -943, ModelingChrome.CHROME_BG);
    }

    private void renderViewport(GuiGraphics guiGraphics) {
        ModelingLayout.Rect rect = this.layout.viewport;
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF151515);
        guiGraphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
        try {
            // 体素在前：内部面已剔除，剩下的靠深度排序。线框随后画并关掉深度测试，
            // 否则画板格线 / 高亮框与体素面共面，深度相同会被零碎剔除（看起来像缺了一半）。
            renderVoxels(guiGraphics);
            renderWalls(guiGraphics);
            if (this.showOriginalShape) {
                renderOriginalShape(guiGraphics);
            }
            renderHover(guiGraphics);
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /**
     * 只画朝外的面：相邻格为空的六个方向，内部面直接跳过。
     * <p>
     * 自己走 {@code position_color} 而不是 {@code RenderType.debugQuads()}：后者的剔除状态不受控制，
     * 而相机把模型 Y 轴翻了过来（右手系变左手系），很容易把正面剔掉、只剩两三个背面。
     */
    private void renderVoxels(GuiGraphics guiGraphics) {
        if (this.grid.isEmpty()) {
            return;
        }
        Matrix4f matrix = viewMatrix();
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
                vertex(builder, matrix, x0, y0, z0, color);
                vertex(builder, matrix, x1, y0, z0, color);
                vertex(builder, matrix, x1, y0, z1, color);
                vertex(builder, matrix, x0, y0, z1, color);
            }
            case UP -> {
                vertex(builder, matrix, x0, y1, z0, color);
                vertex(builder, matrix, x0, y1, z1, color);
                vertex(builder, matrix, x1, y1, z1, color);
                vertex(builder, matrix, x1, y1, z0, color);
            }
            case NORTH -> {
                vertex(builder, matrix, x0, y0, z0, color);
                vertex(builder, matrix, x0, y1, z0, color);
                vertex(builder, matrix, x1, y1, z0, color);
                vertex(builder, matrix, x1, y0, z0, color);
            }
            case SOUTH -> {
                vertex(builder, matrix, x0, y0, z1, color);
                vertex(builder, matrix, x1, y0, z1, color);
                vertex(builder, matrix, x1, y1, z1, color);
                vertex(builder, matrix, x0, y1, z1, color);
            }
            case WEST -> {
                vertex(builder, matrix, x0, y0, z0, color);
                vertex(builder, matrix, x0, y0, z1, color);
                vertex(builder, matrix, x0, y1, z1, color);
                vertex(builder, matrix, x0, y1, z0, color);
            }
            default -> {
                vertex(builder, matrix, x1, y0, z0, color);
                vertex(builder, matrix, x1, y1, z0, color);
                vertex(builder, matrix, x1, y1, z1, color);
                vertex(builder, matrix, x1, y0, z1, color);
            }
        }
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, int color) {
        builder.addVertex(matrix, x, y, z).setColor(color);
    }
    /**
         * 三块画板：{@code x=0} / {@code y=0} / {@code z=0} 三个坐标平面各画 16×16 网格。
     * <p>
         * 它们就是方块的内侧三面（地板 + 左墙 + 后墙），默认视角看进去正好是个敞口盒子，
         * 起笔后笔刷沿法线朝你这边铺，方块内部就是可雕刻的空间。
     */
    private void renderWalls(GuiGraphics guiGraphics) {
        if (!this.showGrid) {
            return;
        }
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        beginOverlay();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(1.0F);
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (int plane = 0; plane < WALL_COUNT; plane++) {
                for (int i = 0; i <= CollisionVoxelGrid.SIZE; i++) {
                    boolean border = i == 0 || i == CollisionVoxelGrid.SIZE;
                    addWallLines(builder, matrix, plane, i * (float) CollisionVoxelGrid.CELL,
                            border ? WALL_BORDER[plane] : WALL_LINE[plane]);
                }
            }
                // 方块本身的 12 条棱：画板都贴在内侧，没有这圈外框就看不出可雕刻的范围
                addBox(builder, matrix, new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), CUBE_OUTLINE);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            endOverlay();
        }
    }

    /** 在某块画板上、距离边缘 {@code offset} 处画两条互相垂直的网格线。 */
    private static void addWallLines(BufferBuilder builder, Matrix4f matrix, int plane, float offset, int color) {
        switch (plane) {
            case WALL_YZ -> {
                    addLine(builder, matrix, 0.0F, offset, 0.0F, 0.0F, offset, 1.0F, color);
                    addLine(builder, matrix, 0.0F, 0.0F, offset, 0.0F, 1.0F, offset, color);
            }
            case WALL_XZ -> {
                    addLine(builder, matrix, offset, 0.0F, 0.0F, offset, 0.0F, 1.0F, color);
                    addLine(builder, matrix, 0.0F, 0.0F, offset, 1.0F, 0.0F, offset, color);
            }
            default -> {
                    addLine(builder, matrix, offset, 0.0F, 0.0F, offset, 1.0F, 0.0F, color);
                    addLine(builder, matrix, 0.0F, offset, 0.0F, 1.0F, offset, 0.0F, color);
            }
        }
    }

    /** 打开界面时的原碰撞箱，只做参考线框。 */
    private void renderOriginalShape(GuiGraphics guiGraphics) {
        VoxelShape shape = this.entity.getCustomShape();
        if (shape == null || shape.isEmpty()) {
            return;
        }
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        beginOverlay();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(1.0F);
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (AABB box : shape.toAabbs()) {
                addBox(builder, matrix, box, ORIGINAL_SHAPE_COLOR);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            endOverlay();
        }
    }

    /** 悬停高亮：当前笔刷将覆盖的体积（半透明填充 + 白色线框），命中格本身（绿色）。 */
    private void renderHover(GuiGraphics guiGraphics) {
        if (!this.voxelHover && this.wallPlane < 0) {
            return;
        }
        int[] range = previewRange();
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        beginOverlay();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            if (range != null) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder fill = tesselator.begin(VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR);
                addRangeFaces(fill, matrix, range, HOVER_PLACE_FILL);
                BufferUploader.drawWithShader(fill.buildOrThrow());
            }
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(2.0F);
            if (this.voxelHover) {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                        DefaultVertexFormat.POSITION_COLOR_NORMAL);
                addCellBox(builder, matrix, this.hoverX, this.hoverY, this.hoverZ, HOVER_REMOVE);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
            if (range != null) {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                        DefaultVertexFormat.POSITION_COLOR_NORMAL);
                addRangeBox(builder, matrix, range, HOVER_PLACE);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
        } finally {
            endOverlay();
        }
    }

    /** 模型空间 → GUI 像素的矩阵：绘制与拾取共用一份，保证画的和点的是同一处。 */
    private Matrix4f viewMatrix() {
        return this.camera.baseMatrix(this.layout.viewport);
    }

    /**
     * 视口里的线框 / 高亮统一关掉深度测试。
     * <p>
     * 它们贴在体素面上（同一深度上的两个图元），开着深度测试会互相剔除，只剩零碎几段；
     * 这些本来就是叠加显示的辅助线，压在最上层才不会被体素挡住。
     */
    private static void beginOverlay() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void endOverlay() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    private void renderCounters(GuiGraphics guiGraphics) {
        ModelingLayout.Rect right = this.layout.right;
        int x = right.x() + 5;
        int y = right.y() + TITLE_HEIGHT + 2 + ROW * 2 + 4;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.yuushya.collision.count", this.grid.count(),
                        CollisionVoxelGrid.CELLS),
                x, y, ModelingChrome.TEXT, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.yuushya.collision.boxes", boxCount()),
                x, y + 12, ModelingChrome.PANEL_TITLE, false);
        String hint = null;
        if (this.wallPlane >= 0 || this.voxelHover) {
            int[] anchor = actionAnchor(true);
            boolean empty = anchor != null && !this.grid.get(anchor[0], anchor[1], anchor[2]);
            hint = Component.translatable(empty
                    ? "gui.yuushya.collision.hover.fill"
                    : "gui.yuushya.collision.hover.erase").getString();
        }
        if (hint != null) {
            guiGraphics.drawString(this.font, hint, x, y + 24, ModelingChrome.PANEL_TITLE, false);
        }
    }

    /** 合并后的盒子数（用来判断形状会不会太碎），按需重算。 */
    private int boxCount() {
        if (this.boxesDirty) {
            this.cachedBoxCount = this.grid.toShape().toAabbs().size();
            this.boxesDirty = false;
        }
        return this.cachedBoxCount;
    }

    private void renderHint(GuiGraphics guiGraphics) {
        ModelingLayout.Rect footer = this.layout.footer;
        Component hint = Component.translatable("gui.yuushya.collision.hint");
        guiGraphics.drawString(this.font, hint, footer.x() + 5, footer.y() + 4, ModelingChrome.TEXT, false);
        Component right = Component.translatable("gui.yuushya.collision.cameraHint");
        guiGraphics.drawString(this.font, right, footer.right() - 5 - this.font.width(right),
                footer.y() + 4, ModelingChrome.PANEL_TITLE, false);
    }

    // ---- 线与盒 ----

    /**
     * 提交一条线段。
     * <p>
     * {@code rendertype_lines} 的顶点着色器把 {@code Normal} 当作「第二个顶点」来探测屏幕方向：
     * {@code ProjMat * ModelViewMat * (Position + Normal)} 再和 {@code Position} 的投影相减，
     * 用两者的 xy 差做线宽扩张。GUI 的 {@code ProjMat} 是正交投影、只吃 xy，所以这里的
     * {@code Normal} 必须是<b>像素空间</b>的方向：若按模型空间传 {@code (0,0,1)}（沿 Z 的线），
     * 探测出的 xy 位移恒为 0，{@code normalize(0)} 得 NaN，整段线直接被丢弃。
     */
    private static void addLine(BufferBuilder builder, Matrix4f matrix,
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
                                           float x0, float y0, float z0, float x1, float y1, float z1) {
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

    private static void addBox(BufferBuilder builder, Matrix4f matrix, AABB box, int color) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        addLine(builder, matrix, x0, y0, z0, x1, y0, z0, color);
        addLine(builder, matrix, x1, y0, z0, x1, y0, z1, color);
        addLine(builder, matrix, x1, y0, z1, x0, y0, z1, color);
        addLine(builder, matrix, x0, y0, z1, x0, y0, z0, color);
        addLine(builder, matrix, x0, y1, z0, x1, y1, z0, color);
        addLine(builder, matrix, x1, y1, z0, x1, y1, z1, color);
        addLine(builder, matrix, x1, y1, z1, x0, y1, z1, color);
        addLine(builder, matrix, x0, y1, z1, x0, y1, z0, color);
        addLine(builder, matrix, x0, y0, z0, x0, y1, z0, color);
        addLine(builder, matrix, x1, y0, z0, x1, y1, z0, color);
        addLine(builder, matrix, x1, y0, z1, x1, y1, z1, color);
        addLine(builder, matrix, x0, y0, z1, x0, y1, z1, color);
    }

    private static void addCellBox(BufferBuilder builder, Matrix4f matrix, int x, int y, int z, int color) {
        double cell = CollisionVoxelGrid.CELL;
        addBox(builder, matrix, new AABB(x * cell, y * cell, z * cell,
                (x + 1) * cell, (y + 1) * cell, (z + 1) * cell), color);
    }

    /** 体素范围内的方框（闭区间坐标）。 */
    private static void addRangeBox(BufferBuilder builder, Matrix4f matrix, int[] range, int color) {
        double cell = CollisionVoxelGrid.CELL;
        addBox(builder, matrix, new AABB(range[0] * cell, range[1] * cell, range[2] * cell,
                (range[3] + 1) * cell, (range[4] + 1) * cell, (range[5] + 1) * cell), color);
    }

    /** 体素范围的六个外表面，用于半透明填充（剔除已关闭，每个面只发一次）。 */
    private static void addRangeFaces(BufferBuilder builder, Matrix4f matrix, int[] range, int color) {
        double cell = CollisionVoxelGrid.CELL;
        float x0 = (float) (range[0] * cell);
        float y0 = (float) (range[1] * cell);
        float z0 = (float) (range[2] * cell);
        float x1 = (float) ((range[3] + 1) * cell);
        float y1 = (float) ((range[4] + 1) * cell);
        float z1 = (float) ((range[5] + 1) * cell);
        quad(builder, matrix, color, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        quad(builder, matrix, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        quad(builder, matrix, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(builder, matrix, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(builder, matrix, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(builder, matrix, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, int color,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3) {
        vertex(builder, matrix, x0, y0, z0, color);
        vertex(builder, matrix, x1, y1, z1, color);
        vertex(builder, matrix, x2, y2, z2, color);
        vertex(builder, matrix, x3, y3, z3, color);
    }

    /** 画板格 → 该列起点体素坐标（贴平面那一格，垂直轴从 0 开始）。 */
    private static int[] wallCellToVoxel(int plane, int u, int v) {
        int top = CollisionVoxelGrid.SIZE - 1;
        return switch (plane) {
                case WALL_YZ -> new int[]{0, top - v, u};
                case WALL_XZ -> new int[]{u, 0, v};
                default -> new int[]{u, top - v, 0};
        };
    }

    /** 画板对应的「垂直轴」。 */
    private static int wallAxis(int plane) {
        return switch (plane) {
            case WALL_YZ -> 0;
            case WALL_XZ -> 1;
            default -> 2;
        };
    }

    // ---- 拾取 ----

    /**
     * 把鼠标像素反投影成射线，分别与体素、三块画板求交，取更近的那个作为编辑目标。
     * <p>
     * 相机矩阵是仿射的，像素 {@code (mx, my)} 对应的模型空间射线就是
     * {@code M⁻¹(mx, my, 0)} → {@code M⁻¹(mx, my, 1)} 两点连线，不需要投影矩阵。
     */
    private void updateHover(double mouseX, double mouseY) {
        this.voxelHover = false;
        this.placeValid = false;
        this.wallPlane = -1;
        if (this.layout == null) {
            return;
        }
        ModelingLayout.Rect rect = this.layout.viewport;
        if (!rect.contains(mouseX, mouseY)) {
            return;
        }
        Matrix4f inverse = new Matrix4f(this.camera.baseMatrix(rect));
        if (Math.abs(inverse.determinant()) < 1.0E-9F) {
            return;
        }
        inverse.invert();
        Vector4f near = inverse.transform(new Vector4f((float) mouseX, (float) mouseY, 0.0F, 1.0F), new Vector4f());
        Vector4f far = inverse.transform(new Vector4f((float) mouseX, (float) mouseY, 1.0F, 1.0F), new Vector4f());
        double dirX = far.x() - near.x();
        double dirY = far.y() - near.y();
        double dirZ = far.z() - near.z();
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 1.0E-9) {
            return;
        }
        dirX /= length;
        dirY /= length;
        dirZ /= length;

        double voxelT = traceVoxel(near.x(), near.y(), near.z(), dirX, dirY, dirZ);
        double wallT = traceWall(near.x(), near.y(), near.z(), dirX, dirY, dirZ);
        if (voxelT >= 0 && (wallT < 0 || voxelT <= wallT)) {
            this.wallPlane = -1;
        } else if (wallT >= 0) {
            this.voxelHover = false;
            this.placeValid = false;
        }
    }

    /**
     * 在 [0,1]³ 里步进找第一个实心格。
     *
     * @return 射线进入该格的 t；没命中返回 -1
     */
    private double traceVoxel(double originX, double originY, double originZ,
                              double dirX, double dirY, double dirZ) {
        double[] enter = new double[1];
        double[] exit = new double[1];
        if (!clipToUnitCube(originX, originY, originZ, dirX, dirY, dirZ, enter, exit)) {
            return -1;
        }
        int cellX = CollisionVoxelGrid.cellOf(originX + dirX * enter[0]);
        int cellY = CollisionVoxelGrid.cellOf(originY + dirY * enter[0]);
        int cellZ = CollisionVoxelGrid.cellOf(originZ + dirZ * enter[0]);

        int stepX = dirX > 0 ? 1 : -1;
        int stepY = dirY > 0 ? 1 : -1;
        int stepZ = dirZ > 0 ? 1 : -1;
        double cellSize = CollisionVoxelGrid.CELL;

        double maxX = Math.abs(dirX) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellX + (stepX > 0 ? 1 : 0)) * cellSize - originX) / dirX;
        double maxY = Math.abs(dirY) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellY + (stepY > 0 ? 1 : 0)) * cellSize - originY) / dirY;
        double maxZ = Math.abs(dirZ) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellZ + (stepZ > 0 ? 1 : 0)) * cellSize - originZ) / dirZ;
        double deltaX = Math.abs(dirX) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(dirX);
        double deltaY = Math.abs(dirY) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(dirY);
        double deltaZ = Math.abs(dirZ) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(dirZ);

        int previousX = -1;
        int previousY = -1;
        int previousZ = -1;
        int entryAxis = 0;
        double cellEntryT = enter[0];

        for (int step = 0; step < CollisionVoxelGrid.SIZE * 3 + 3; step++) {
            if (this.grid.get(cellX, cellY, cellZ)) {
                this.voxelHover = true;
                this.hoverX = cellX;
                this.hoverY = cellY;
                this.hoverZ = cellZ;
                this.hoverFaceAxis = entryAxis;
                this.placeValid = CollisionVoxelGrid.inside(previousX, previousY, previousZ)
                        && !this.grid.get(previousX, previousY, previousZ);
                this.placeX = previousX;
                this.placeY = previousY;
                this.placeZ = previousZ;
                return cellEntryT;
            }
            previousX = cellX;
            previousY = cellY;
            previousZ = cellZ;

            double next = Math.min(maxX, Math.min(maxY, maxZ));
            if (next > exit[0]) {
                return -1;
            }
            if (maxX <= maxY && maxX <= maxZ) {
                cellX += stepX;
                maxX += deltaX;
                entryAxis = 0;
            } else if (maxY <= maxZ) {
                cellY += stepY;
                maxY += deltaY;
                entryAxis = 1;
            } else {
                cellZ += stepZ;
                maxZ += deltaZ;
                entryAxis = 2;
            }
            cellEntryT = next;
            if (!CollisionVoxelGrid.inside(cellX, cellY, cellZ)) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * 与三块画板求交，取最近的一块并把命中的格子记下来。
     *
     * @return 命中处的 t；没命中返回 -1
     */
    private double traceWall(double originX, double originY, double originZ,
                             double dirX, double dirY, double dirZ) {
        double best = -1.0;
        int bestPlane = -1;
        int bestU = 0;
        int bestV = 0;
        for (int plane = 0; plane < WALL_COUNT; plane++) {
            double dir = switch (plane) {
                case WALL_YZ -> dirX;
                case WALL_XZ -> dirY;
                default -> dirZ;
            };
            double origin = switch (plane) {
                case WALL_YZ -> originX;
                case WALL_XZ -> originY;
                default -> originZ;
            };
            if (Math.abs(dir) < 1.0E-9) {
                continue;
            }
                        // 画板位于方块内侧的坐标平面（x=0 / y=0 / z=0）
                        double t = -origin / dir;
            if (t < 0.0 || (best >= 0.0 && t >= best)) {
                continue;
            }
            double hitX = originX + dirX * t;
            double hitY = originY + dirY * t;
            double hitZ = originZ + dirZ * t;
            if (hitX < -1.0E-4 || hitX > 1.0 + 1.0E-4
                    || hitY < -1.0E-4 || hitY > 1.0 + 1.0E-4
                    || hitZ < -1.0E-4 || hitZ > 1.0 + 1.0E-4) {
                continue;
            }
            int u;
            int v;
            switch (plane) {
                case WALL_YZ -> {
                    u = CollisionVoxelGrid.cellOf(hitZ);
                    v = CollisionVoxelGrid.SIZE - 1 - CollisionVoxelGrid.cellOf(hitY);
                }
                case WALL_XZ -> {
                    u = CollisionVoxelGrid.cellOf(hitX);
                    v = CollisionVoxelGrid.cellOf(hitZ);
                }
                default -> {
                    u = CollisionVoxelGrid.cellOf(hitX);
                    v = CollisionVoxelGrid.SIZE - 1 - CollisionVoxelGrid.cellOf(hitY);
                }
            }
            best = t;
            bestPlane = plane;
            bestU = u;
            bestV = v;
        }
        if (bestPlane >= 0) {
            this.wallPlane = bestPlane;
            this.wallCellU = bestU;
            this.wallCellV = bestV;
        }
        return best;
    }

    /** 射线与 [0,1]³ 的相交区间；不直接穿入时返回 {@code false}。 */
    private static boolean clipToUnitCube(double originX, double originY, double originZ,
                                         double dirX, double dirY, double dirZ,
                                         double[] enter, double[] exit) {
        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        double[] origins = {originX, originY, originZ};
        double[] dirs = {dirX, dirY, dirZ};
        for (int axis = 0; axis < 3; axis++) {
            double origin = origins[axis];
            double dir = dirs[axis];
            if (Math.abs(dir) < 1.0E-9) {
                if (origin < 0.0 || origin > 1.0) {
                    return false;
                }
                continue;
            }
            double t1 = (0.0 - origin) / dir;
            double t2 = (1.0 - origin) / dir;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        }
        if (tMax < 0.0) {
            return false;
        }
        enter[0] = Math.max(tMin, 0.0);
        exit[0] = tMax;
        return true;
    }

    // ---- 输入 ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.layout == null) {
            return false;
        }
        if (this.axisIndicator.mouseClicked(this.layout.viewport, mouseX, mouseY)) {
            return true;
        }
        if (!this.layout.viewport.contains(mouseX, mouseY)) {
            return false;
        }
        this.pressX = mouseX;
        this.pressY = mouseY;
        this.pressButton = button;
        this.dragged = false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.gesture = GESTURE_ORBIT;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.gesture = GESTURE_PAN;
        } else {
            this.gesture = GESTURE_NONE;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.gesture == GESTURE_NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (!this.dragged) {
            this.dragged = Math.abs(mouseX - this.pressX) > DRAG_THRESHOLD
                    || Math.abs(mouseY - this.pressY) > DRAG_THRESHOLD;
        }
        if (this.dragged) {
            if (this.gesture == GESTURE_ORBIT) {
                this.camera.orbit(dragX, dragY);
            } else {
                this.camera.pan(dragX, dragY);
            }
        }
        return true;
    }

    /**
     * 单击执行编辑：左键按笔刷放置，右键按笔刷挖除。
     * <p>
     * 拖动过视角就不触发编辑，避免转视角时误改数据。
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.gesture == GESTURE_NONE) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        this.gesture = GESTURE_NONE;
        if (!this.dragged && button == this.pressButton) {
            updateHover(mouseX, mouseY);
            boolean place = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            boolean erase = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            if (place || erase) {
                int[] anchor = actionAnchor(place);
                if (anchor != null) {
                    paintBrush(anchor, place);
                }
            }
        }
        this.pressButton = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.layout != null && this.layout.viewport.contains(mouseX, mouseY) && scrollY != 0.0) {
            this.camera.zoom(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrl = hasControlDown();
        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) {
            if (hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_S) {
            apply();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}