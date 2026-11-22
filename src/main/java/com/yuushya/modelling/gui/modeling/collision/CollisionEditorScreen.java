package com.yuushya.modelling.gui.modeling.collision;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.gui.modeling.AbstractModelingScreen;
import com.yuushya.modelling.gui.modeling.ModelingCamera;
import com.yuushya.modelling.gui.modeling.ModelingChrome;
import com.yuushya.modelling.gui.modeling.ModelingLayout;
import com.yuushya.modelling.gui.modeling.ModelingRender;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

/**
 * 碰撞箱雕刻界面：在 3D 视口里把碰撞箱当成 16³ 体素来搭。
 * <p>
 * 三块「画板」就是方块局部空间的三个坐标平面 —— {@code x = 0}（YZ 面）、{@code y = 0}（XZ 面，地板）、
 * {@code z = 0}（XY 面，背墙），各画成 16×16 网格。在画板上点一格，就把当前笔刷形状从该面往方块内部放，
 * 于是从地板往上垒、从背墙往外补，像搭积木一样叠出碰撞箱。
 * <p>
 * 笔刷形状见 {@link BrushMode}：丁状（单格）、1/8、1/4、1/2 方块、条状、板状，以及方框。
 * <p>
 * 交互（与 Minecraft 的放置 / 破坏一致）：
 * <ul>
 *   <li>左键：按当前笔刷放置；右键：按当前笔刷挖除；都是点一下落一次笔</li>
 *   <li>方框笔刷：按住拖动用与普通笔刷相同的选点划出方框，松开时才填充 / 清除整框</li>
 *   <li>左键拖动环绕视角、右键拖动平移、中键拖动环绕、滚轮缩放</li>
 *   <li>方框笔刷拖动时会占用左右键，此时按住 Alt 拖动才是视角操作</li>
 *   <li>Ctrl+Z / Ctrl+Y 撤销 / 重做；Esc 取消</li>
 * </ul>
 * 方框松开时才提交，一次拖动只压一份撤销快照。
 * <p>
 * 拾取以「光标射线实际穿过的笔刷体积」为准：优先已有实心格，其次上一帧仍压在光标下的预览框，
 * 最后才落到三张零坐标画板，保证画出来的框就是点下去会改的格子。
 * <p>
 * 编辑的是一份 {@link CollisionVoxelGrid}，点「应用」时才通过 {@link CustomShapePacket} 写回服务端；
 * 方块形状一般设为 {@code CUSTOM} 才会参与判定；雕出的形状与某个预设完全一致时退化为该预设。
 */
public final class CollisionEditorScreen extends AbstractModelingScreen {

    private static final int BUTTON_HEIGHT = 18;
    /**
     * 反投影射线在屏幕 z 方向上的半长。
     * <p>
     * GUI 的 z 越大越靠近观察者（GuiRenderer 用 ortho 1000..11000 后再平移 -11000），
     * 所以起点取 {@code +RAY_DEPTH}（观察者一侧），终点取 {@code -RAY_DEPTH}（画面深处）。
     * <p>
     * 数值要足够大：GUI 的 z 跨度换算到模型空间要除以缩放系数（{@code min(宽,高) * 0.72 / distance}），
     * 放大到极限时模型能占到 ±数千的 z 范围，取小了会让「射线」整段落在方块内部。
     */
    private static final float RAY_DEPTH = 20000.0F;
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
    private static final int HOVER_REMOVE_FILL = 0x405AD67D;

    private final Screen parent;
    private final AbstractTransformBlockEntity entity;
    private final CollisionVoxelGrid grid;
    private final Deque<long[]> undoStack = new ArrayDeque<>();
    private final Deque<long[]> redoStack = new ArrayDeque<>();

    private final List<AbstractWidget> panels = new ArrayList<>();

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
    private int pressButton = -1;
    /** 按下瞬间的落笔锚点（非方框笔刷）：抬手没拖动时用它落笔。 */
    private int[] pressAnchor;

    // ---- 方框拖拽（仅方框笔刷） ----
    /** 正在拖方框的鼠标键（左 / 右），{@code -1} 表示没有。 */
    private int rectButton = -1;
    /** 拖动开始前的网格快照：整框只压一次撤销。 */
    private long[] rectSnapshot;
    /** 方框的两个角（体素锚点）；没有方框时为 {@code null}。 */
    private int[] rectStart;
    private int[] rectEnd;

    /** 射线在三个轴上的步进方向（±1）：给命中格找相邻空格时用来判断「往里」是哪边。 */
    private int hoverStepX = 1;
    private int hoverStepY = 1;
    private int hoverStepZ = 1;
    /**
     * 当前帧解析出的放置 / 挖除锚点。
     * <p>
     * 预览框、点击落笔和下一帧的「粘滞」判定都用这一份结果，保证看到的框就是会改的格子。
     */
    private int[] placeTarget;
    private int[] eraseTarget;

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
        this.grid.fromShape(effectiveShape(entity));
        this.camera.orbit(INITIAL_ORBIT_DEGREES / ORBIT_DEGREES_PER_PIXEL, 0.0);
    }

    /** 当前真正生效的碰撞箱：CUSTOM 用实体里的自定义形状，其余用方块状态的预设形状。 */
    private static VoxelShape effectiveShape(AbstractTransformBlockEntity entity) {
        BlockShape preset = entity.getBlockState().getValue(SHAPES);
        if (preset != BlockShape.CUSTOM) {
            return preset.voxelShape;
        }
        VoxelShape custom = entity.getCustomShape();
        return custom == null ? Shapes.empty() : custom;
    }

    /**
     * 笔刷形状：点一下要覆盖多大的体积。
     * <p>
     * 三个「方块比例」模式的边长按方块算（1/8 方块 = 2 格，1/4 = 4 格，1/2 = 8 格，居中展开）；
          * 条状沿当前平面 / 命中面的法线方向贯通整条；板状铺满除法线以外的两个方向；
          * 方框则是按住拖动划出范围，松开时才一次性填充 / 清除。
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
        BAR(1),
        /** 垂直于主轴铺满的一层。 */
        PLATE(0),
        /** 按住拖动划出方框，松开后填充 / 清除整个方框。 */
        RECT(1);

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
    protected void initScreen() {
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
            pushUndo(this.grid.snapshot());
        }

        /** 压入一份已经拍好的快照（一笔拖动只在第一次真正改动时压一次）。 */
        private void pushUndo(long[] snapshot) {
        if (this.undoStack.size() >= UNDO_LIMIT) {
            this.undoStack.removeLast();
        }
            this.undoStack.push(snapshot);
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
            int size = Math.clamp(sizes[axis], 1, CollisionVoxelGrid.SIZE);
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
     * 左键（放置）候选：命中面外侧那格优先，边界处退回往里一格，最后退回命中格本身。
     * <p>
     * 每个候选都要通过「笔刷体积被射线穿过」的检查，保证预览框始终压在光标下；
     * 大笔刷伸出碰撞箱轮廓的部分也因此可以继续点击。
     */
    private int[] voxelPlaceTarget(double originX, double originY, double originZ,
                                   double dirX, double dirY, double dirZ) {
        if (!this.voxelHover) {
            return null;
        }
        if (this.placeValid) {
            int[] outward = {this.placeX, this.placeY, this.placeZ, this.hoverFaceAxis};
            if (rangeHit(outward, originX, originY, originZ, dirX, dirY, dirZ)) {
                return outward;
            }
        }
        int[] inward = inwardFreeCell();
        if (inward != null && rangeHit(inward, originX, originY, originZ, dirX, dirY, dirZ)) {
            return inward;
        }
        int[] hit = {this.hoverX, this.hoverY, this.hoverZ, this.hoverFaceAxis};
        return rangeHit(hit, originX, originY, originZ, dirX, dirY, dirZ) ? hit : null;
    }

    /**
     * 命中格「往里」那一格（沿射线穿过该面的方向），仅在它是空格时返回。
     * <p>
     * 命中面正好贴在 16³ 边界上时，外侧那一格在体积之外、无处可放；这时退一步填到方块内部，
     * 至少能让左键在贴着边界的表面上继续加料，而不是完全没反应。
     */
    private int[] inwardFreeCell() {
        int axis = this.hoverFaceAxis;
        int step = axis == 0 ? this.hoverStepX : axis == 1 ? this.hoverStepY : this.hoverStepZ;
        int x = this.hoverX + (axis == 0 ? step : 0);
        int y = this.hoverY + (axis == 1 ? step : 0);
        int z = this.hoverZ + (axis == 2 ? step : 0);
        if (CollisionVoxelGrid.inside(x, y, z) && !this.grid.get(x, y, z)) {
            return new int[]{x, y, z, axis};
        }
        return null;
    }

    /** 右键（挖除）候选：命中格本身。 */
    private int[] voxelEraseTarget(double originX, double originY, double originZ,
                                   double dirX, double dirY, double dirZ) {
        if (!this.voxelHover) {
            return null;
        }
        int[] hit = {this.hoverX, this.hoverY, this.hoverZ, this.hoverFaceAxis};
        return rangeHit(hit, originX, originY, originZ, dirX, dirY, dirZ) ? hit : null;
    }

    /** 画板格 → 贴平面那一格；没有命中画板时返回 {@code null}。 */
    private int[] wallAnchor() {
        if (this.wallPlane < 0) {
            return null;
        }
        int[] cell = wallCellToVoxel(this.wallPlane, this.wallCellU, this.wallCellV);
        return new int[]{cell[0], cell[1], cell[2], wallAxis(this.wallPlane)};
    }

    /** 画板候选：只有落在光标下的画板格才接受。 */
    private int[] wallTarget(double originX, double originY, double originZ,
                             double dirX, double dirY, double dirZ) {
        int[] wall = wallAnchor();
        if (wall == null) {
            return null;
        }
        return rangeHit(wall, originX, originY, originZ, dirX, dirY, dirZ) ? wall : null;
    }

    /**
     * 解析一帧的落笔锚点。
     * <p>
     * 优先级：已有实心格 → 上一帧仍压在光标下的预览框 → 零坐标画板。
     * 「上一帧的预览框」让大笔刷伸出去的那部分也能点中，同时不会把画板上的目标顶掉；
     * 没有候选时返回 {@code null}，界面就不画框，点击也不生效。
     */
    private int[] resolveTarget(boolean place, double originX, double originY, double originZ,
                                double dirX, double dirY, double dirZ, int[] previous) {
        int[] voxel = place
                ? voxelPlaceTarget(originX, originY, originZ, dirX, dirY, dirZ)
                : voxelEraseTarget(originX, originY, originZ, dirX, dirY, dirZ);
        if (voxel != null) {
            return voxel;
        }
        // 画板格必须跟着光标走；只有光标已经离开画板/体素时，才允许沿用上一帧的框
        boolean overWall = !this.voxelHover && this.wallPlane >= 0;
        if (!overWall && previous != null
                && rangeHit(previous, originX, originY, originZ, dirX, dirY, dirZ)) {
            return previous;
        }
        return wallTarget(originX, originY, originZ, dirX, dirY, dirZ);
    }

    /** 从该锚点展开的笔刷体积是否被射线穿过。 */
    private boolean rangeHit(int[] anchor, double originX, double originY, double originZ,
                             double dirX, double dirY, double dirZ) {
        return rayBoxEntry(originX, originY, originZ, dirX, dirY, dirZ,
                brushRange(anchor, anchor[3])) >= 0.0;
    }

    /** 指定的格子范围（闭区间）里是否有格子需要改成 {@code filled}。 */
    private boolean needsPaintRange(int[] range, boolean filled) {
        for (int x = range[0]; x <= range[3]; x++) {
            for (int y = range[1]; y <= range[4]; y++) {
                for (int z = range[2]; z <= range[5]; z++) {
                    if (this.grid.get(x, y, z) != filled) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 把指定格子范围（闭区间）整体写成 {@code filled}。 */
    private void stampRange(int[] range, boolean filled) {
        boolean changed = false;
        for (int x = range[0]; x <= range[3]; x++) {
            for (int y = range[1]; y <= range[4]; y++) {
                for (int z = range[2]; z <= range[5]; z++) {
                    if (this.grid.set(x, y, z, filled) != filled) {
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            this.boxesDirty = true;
        }
    }

    /**
     * 单击落笔（除方框外的笔刷）：整块一次写入，一次撤销。
     * <p>
     * 先检测范围内是否真有格子需要改，避免"点一下没变化也压一份撤销"。
     */
    private void paintBrush(int[] anchor, boolean filled) {
        int[] range = brushRange(anchor, anchor[3]);
        if (!needsPaintRange(range, filled)) {
            return;
        }
        pushUndo();
        stampRange(range, filled);
    }

    // ---- 方框拖拽 ----

    /** 开始拖方框：记下第一个角与快照，松开时才提交。 */
    private void beginRect(int button, int[] anchor) {
        this.rectButton = button;
        this.rectSnapshot = this.grid.snapshot();
        this.rectStart = new int[]{anchor[0], anchor[1], anchor[2], anchor[3]};
        this.rectEnd = this.rectStart.clone();
    }

    /** 结束方框拖动。 */
    private void endRect() {
        this.rectButton = -1;
        this.rectSnapshot = null;
        this.rectStart = null;
        this.rectEnd = null;
    }

    /** 方框拖动：用与普通笔刷相同的选点更新另一个角。 */
    private void updateRect(double mouseX, double mouseY) {
        boolean place = this.rectButton == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        int[] anchor = pickTarget(mouseX, mouseY, place);
        if (anchor != null) {
            this.rectEnd = anchor;
        }
    }

    /** 两个角围成的格子范围（闭区间）；没有方框时返回 {@code null}。 */
    private int[] rectRange() {
        if (this.rectStart == null || this.rectEnd == null) {
            return null;
        }
        int[] min = new int[3];
        int[] max = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            int a = this.rectStart[axis];
            int b = this.rectEnd[axis];
            min[axis] = Math.min(a, b);
            max[axis] = Math.max(a, b);
        }
        return new int[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    /** 松开方框时提交：整框填充 / 清除，只算一步撤销。 */
    private void commitRect(boolean place) {
        int[] range = rectRange();
        if (range == null || !needsPaintRange(range, place)) {
            return;
        }
        pushUndo(this.rectSnapshot);
        stampRange(range, place);
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

    // ---- 渲染 ----

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ensureLayout();
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        updateHover(mouseX, mouseY);
        renderViewport(guiGraphics);
        renderAxisIndicator(guiGraphics, mouseX, mouseY);
        ModelingChrome.render(guiGraphics, this.font, this.layout, this.entity.getBlockState().getBlock().getName(),
                null, "gui.yuushya.collision.tools", "gui.yuushya.collision.options");
        renderCounters(guiGraphics);
        renderHint(guiGraphics);
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
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
    private void renderWalls(GuiGraphics guiGraphics) {
        if (!this.showGrid) {
            return;
        }
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        ModelingRender.beginOverlay();
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
                ModelingRender.box(builder, matrix, new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), CUBE_OUTLINE);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            ModelingRender.endOverlay();
        }
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
    private void renderOriginalShape(GuiGraphics guiGraphics) {
        VoxelShape shape = effectiveShape(this.entity);
        if (shape == null || shape.isEmpty()) {
            return;
        }
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        ModelingRender.beginOverlay();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(1.0F);
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (AABB box : shape.toAabbs()) {
                ModelingRender.box(builder, matrix, box, ORIGINAL_SHAPE_COLOR);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            ModelingRender.endOverlay();
        }
    }

    /**
     * 悬停高亮：白色 = 左键放置的实际体积（半透明填充 + 线框），绿色 = 右键挖除的实际体积。
     * <p>
     * 普通笔刷取 {@link #placeTarget} / {@link #eraseTarget}，和点击落笔完全同源；
     * 方框笔刷在拖动期间改用 {@link #rectRange()}，画出来的就是松开后会提交的那一整块。
     */
    private void renderHover(GuiGraphics guiGraphics) {
        int[] placeRange;
        int[] eraseRange;
        if (this.rectButton >= 0) {
            boolean place = this.rectButton == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            int[] range = rectRange();
            placeRange = place ? range : null;
            eraseRange = place ? null : range;
        } else {
            placeRange = this.placeTarget == null
                    ? null : brushRange(this.placeTarget, this.placeTarget[3]);
            eraseRange = this.eraseTarget == null
                    ? null : brushRange(this.eraseTarget, this.eraseTarget[3]);
        }
        if (placeRange == null && eraseRange == null) {
            return;
        }
        Matrix4f matrix = viewMatrix();
        guiGraphics.flush();
        ModelingRender.beginOverlay();
        Tesselator tesselator = Tesselator.getInstance();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            if (placeRange != null) {
                BufferBuilder fill = tesselator.begin(VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR);
                addRangeFaces(fill, matrix, placeRange, HOVER_PLACE_FILL);
                BufferUploader.drawWithShader(fill.buildOrThrow());
            } else if (eraseRange != null) {
                BufferBuilder fill = tesselator.begin(VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR);
                addRangeFaces(fill, matrix, eraseRange, HOVER_REMOVE_FILL);
                BufferUploader.drawWithShader(fill.buildOrThrow());
            }
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.lineWidth(2.0F);
            if (eraseRange != null) {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                        DefaultVertexFormat.POSITION_COLOR_NORMAL);
                addRangeBox(builder, matrix, eraseRange, HOVER_REMOVE);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
            if (placeRange != null) {
                BufferBuilder builder = tesselator.begin(VertexFormat.Mode.LINES,
                        DefaultVertexFormat.POSITION_COLOR_NORMAL);
                addRangeBox(builder, matrix, placeRange, HOVER_PLACE);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
        } finally {
            ModelingRender.endOverlay();
        }
    }

    /** 模型空间 → GUI 像素的矩阵：绘制与拾取共用一份，保证画的和点的是同一处。 */
    private Matrix4f viewMatrix() {
        return this.camera.baseMatrix(this.layout.viewport);
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
        if (this.rectButton >= 0) {
            int[] range = rectRange();
            if (range != null) {
                int volume = (range[3] - range[0] + 1) * (range[4] - range[1] + 1) * (range[5] - range[2] + 1);
                boolean place = this.rectButton == GLFW.GLFW_MOUSE_BUTTON_LEFT;
                hint = Component.translatable(place
                        ? "gui.yuushya.collision.rect.fill"
                        : "gui.yuushya.collision.rect.erase", volume).getString();
            }
        } else if (this.placeTarget != null) {
            boolean empty = !this.grid.get(this.placeTarget[0], this.placeTarget[1], this.placeTarget[2]);
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
        renderFooterHints(guiGraphics,
                Component.translatable("gui.yuushya.collision.hint"),
                Component.translatable("gui.yuushya.collision.cameraHint"));
    }

    /** 体素范围内的方框（闭区间坐标）。 */
    private static void addRangeBox(BufferBuilder builder, Matrix4f matrix, int[] range, int color) {
        double cell = CollisionVoxelGrid.CELL;
        ModelingRender.box(builder, matrix, new AABB(range[0] * cell, range[1] * cell, range[2] * cell,
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
        ModelingRender.quad(builder, matrix, color, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        ModelingRender.quad(builder, matrix, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        ModelingRender.quad(builder, matrix, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        ModelingRender.quad(builder, matrix, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        ModelingRender.quad(builder, matrix, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        ModelingRender.quad(builder, matrix, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
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
     * 把鼠标像素反投影成模型空间射线。
     * <p>
     * 相机矩阵是仿射的，像素 {@code (mx, my)} 对应的模型空间射线就是
     * {@code M⁻¹(mx, my, +RAY_DEPTH)} → {@code M⁻¹(mx, my, -RAY_DEPTH)} 两点连线（正交投影下与投影矩阵无关）。
     * GUI 的 z 越大越靠近观察者，所以起点在观察者一侧。
     * <p>
     * 深度必须取得足够大：GUI 的 z 跨度换算到模型空间要除以缩放系数，放大到极限时
     * 1 像素只对应千分之几个方块，取小了整段「射线」都会落在方块内部。
     *
     * @return {@code {originX, originY, originZ, dirX, dirY, dirZ}}；不可用时返回 {@code null}
     */
    private double[] castRay(double mouseX, double mouseY) {
        if (this.layout == null || !this.layout.viewport.contains(mouseX, mouseY)) {
            return null;
        }
        Matrix4f inverse = new Matrix4f(this.camera.baseMatrix(this.layout.viewport));
        if (Math.abs(inverse.determinant()) < 1.0E-9F) {
            return null;
        }
        inverse.invert();
        Vector4f near = inverse.transform(
                new Vector4f((float) mouseX, (float) mouseY, RAY_DEPTH, 1.0F), new Vector4f());
        Vector4f far = inverse.transform(
                new Vector4f((float) mouseX, (float) mouseY, -RAY_DEPTH, 1.0F), new Vector4f());
        double dirX = far.x() - near.x();
        double dirY = far.y() - near.y();
        double dirZ = far.z() - near.z();
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 1.0E-9) {
            return null;
        }
        return new double[]{near.x(), near.y(), near.z(), dirX / length, dirY / length, dirZ / length};
    }

    /**
     * 在指定像素位置做一次完整拾取，返回该点的新锚点（不做上一帧的「粘滞」）。
     * <p>
     * 拖动画笔时逐点采样都走这里，因此笔迹始终贴着当前光标下的表面。
     */
    private int[] pickTarget(double mouseX, double mouseY, boolean place) {
        double[] ray = castRay(mouseX, mouseY);
        if (ray == null) {
            return null;
        }
        this.voxelHover = false;
        this.placeValid = false;
        this.wallPlane = -1;
        traceVoxel(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
        traceWall(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
        if (this.voxelHover) {
            // 画板只是「画布」：射线碰到实体格时一律编辑实体格
            this.wallPlane = -1;
        } else {
            this.placeValid = false;
        }
        return resolveTarget(place, ray[0], ray[1], ray[2], ray[3], ray[4], ray[5], null);
    }

    /**
     * 刷新悬停预览：解析出「光标真正压住的笔刷体积」。
     * <p>
     * 解析顺序见 {@link #resolveTarget}：已有实心格 → 上一帧仍压在光标下的预览框 → 零坐标画板。
     * 每个候选都要求笔刷体积真的被射线穿过，所以预览框不会画到光标之外，
     * 大笔刷伸出去的部分也能直接点中。
     */
    private void updateHover(double mouseX, double mouseY) {
        // 拖方框时不做「粘滞」，预览直接跟随光标，和松手提交的范围保持同源
        int[] previousPlace = this.rectButton < 0 ? this.placeTarget : null;
        int[] previousErase = this.rectButton < 0 ? this.eraseTarget : null;
        this.voxelHover = false;
        this.placeValid = false;
        this.wallPlane = -1;
        this.placeTarget = null;
        this.eraseTarget = null;
        double[] ray = castRay(mouseX, mouseY);
        if (ray == null) {
            return;
        }
        traceVoxel(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
        traceWall(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
        if (this.voxelHover) {
            // 画板只是「画布」：射线碰到实体格时一律编辑实体格
            this.wallPlane = -1;
        } else {
            this.placeValid = false;
        }
        this.placeTarget = resolveTarget(true, ray[0], ray[1], ray[2], ray[3], ray[4], ray[5], previousPlace);
        this.eraseTarget = resolveTarget(false, ray[0], ray[1], ray[2], ray[3], ray[4], ray[5], previousErase);
    }

    /**
     * 在 [0,1]³ 里步进找第一个实心格。
     */
    private void traceVoxel(double originX, double originY, double originZ,
                            double dirX, double dirY, double dirZ) {
        double[] enter = new double[1];
        double[] exit = new double[1];
        if (!clipToUnitCube(originX, originY, originZ, dirX, dirY, dirZ, enter, exit)) {
            return;
        }
        int cellX = CollisionVoxelGrid.cellOf(originX + dirX * enter[0]);
        int cellY = CollisionVoxelGrid.cellOf(originY + dirY * enter[0]);
        int cellZ = CollisionVoxelGrid.cellOf(originZ + dirZ * enter[0]);

        int stepX = dirX > 0 ? 1 : -1;
        int stepY = dirY > 0 ? 1 : -1;
        int stepZ = dirZ > 0 ? 1 : -1;
                this.hoverStepX = stepX;
                this.hoverStepY = stepY;
                this.hoverStepZ = stepZ;
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
                return;
            }
            previousX = cellX;
            previousY = cellY;
            previousZ = cellZ;

            double next = Math.min(maxX, Math.min(maxY, maxZ));
            if (next > exit[0]) {
                return;
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
                return;
            }
        }
    }

    /**
     * 与三块画板求交，取最近的一块并把命中的格子记下来。
     */
    private void traceWall(double originX, double originY, double originZ,
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

    /** 射线穿过格子范围（闭区间）的进入参数；未相交返回 -1。 */
    private static double rayBoxEntry(double originX, double originY, double originZ,
                                      double dirX, double dirY, double dirZ, int[] range) {
        double cell = CollisionVoxelGrid.CELL;
        return clipRayToBox(originX, originY, originZ, dirX, dirY, dirZ,
                range[0] * cell, range[1] * cell, range[2] * cell,
                (range[3] + 1) * cell, (range[4] + 1) * cell, (range[5] + 1) * cell);
    }

    /** 射线与任意轴对齐盒的进入参数；未相交返回 -1。 */
    private static double clipRayToBox(double originX, double originY, double originZ,
                                       double dirX, double dirY, double dirZ,
                                       double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ) {
        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        double[] origins = {originX, originY, originZ};
        double[] dirs = {dirX, dirY, dirZ};
        double[] mins = {minX, minY, minZ};
        double[] maxs = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double origin = origins[axis];
            double dir = dirs[axis];
            if (Math.abs(dir) < 1.0E-9) {
                if (origin < mins[axis] || origin > maxs[axis]) {
                    return -1.0;
                }
                continue;
            }
            double t1 = (mins[axis] - origin) / dir;
            double t2 = (maxs[axis] - origin) / dir;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return -1.0;
            }
        }
        if (tMax < 0.0) {
            return -1.0;
        }
        return Math.max(tMin, 0.0);
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
        this.pressButton = button;
        this.pressAnchor = null;

        boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        boolean paintButton = (left || right) && !hasAltDown();
        int[] anchor = null;
        if (paintButton) {
            // 用当前帧画出来的锚点起笔：看到哪个框就从哪块开始
            anchor = left ? this.placeTarget : this.eraseTarget;
            if (anchor == null) {
                anchor = pickTarget(mouseX, mouseY, left);
            }
        }

        // 方框笔刷：按住拖动划范围，松开才提交，期间不操作视角
        if (paintButton && anchor != null && this.brush == BrushMode.RECT) {
            beginRect(button, anchor);
            return true;
        }

        // 其它笔刷：只记下落笔点，抬手没拖动时才落一次笔
        this.pressAnchor = anchor;

        beginCameraGesture(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.rectButton >= 0) {
            updateRect(mouseX, mouseY);
            return true;
        }
        if (dragCamera(mouseX, mouseY, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 抬手：方框在这里提交；其它笔刷只在「没有拖动视角」时按一次笔刷。
     * <p>
     * 拖动过视角就不触发编辑，避免转视角时误改数据。
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.rectButton >= 0) {
            updateRect(mouseX, mouseY);
            commitRect(this.rectButton == GLFW.GLFW_MOUSE_BUTTON_LEFT);
            endRect();
            this.pressAnchor = null;
            this.pressButton = -1;
            return true;
        }
        boolean moved = cameraMoved();
        if (!endCameraGesture()) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (!moved && button == this.pressButton && this.pressAnchor != null) {
            paintBrush(this.pressAnchor, button == GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }
        this.pressAnchor = null;
        this.pressButton = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (zoomCamera(mouseX, mouseY, scrollY)) {
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