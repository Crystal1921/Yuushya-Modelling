package com.yuushya.modelling.gui.modeling.collision;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.gui.modeling.AbstractModelingScreen;
import com.yuushya.modelling.gui.modeling.ModelingChrome;
import com.yuushya.modelling.gui.modeling.ModelingLayout;
import com.yuushya.modelling.network.CustomShapePacket;
import com.yuushya.modelling.utils.UndoHistory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 碰撞箱雕刻界面：在 3D 视口里把碰撞箱当成 16³ 体素来搭。
 * <p>
 * 三块「画板」就是方块局部空间的三个坐标平面 —— {@code x = 0}（YZ 面）、{@code y = 0}（XZ 面，地板）、
 * {@code z = 0}（XY 面，背墙），各画成 16×16 网格。在画板上点一格，就把当前笔刷形状从该面往方块内部放，
 * 于是从地板往上垒、从背墙往外补，像搭积木一样叠出碰撞箱。
 * <p>
 * 笔刷形状见 {@link CollisionBrush}：丁状（单格）、1/8、1/4、1/2 方块、条状、板状，以及方框。
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
 * 拾取在 {@link CollisionPicker}、3D 绘制在 {@link CollisionViewportRenderer}、撤销记录在 {@link UndoHistory}，
 * 本类只负责手势、面板，以及把这三者串起来。
 * <p>
 * 编辑的是一份 {@link CollisionVoxelGrid}，点「应用」时才通过 {@link CustomShapePacket} 写回服务端；
 * 方块形状一般设为 {@code CUSTOM} 才会参与判定；雕出的形状与某个预设完全一致时退化为该预设。
 */
public final class CollisionEditorScreen extends AbstractModelingScreen {

    private static final int BUTTON_HEIGHT = 18;
    private static final int UNDO_LIMIT = 64;

    /**
     * 模拟「打开界面时摄像机已经转过 90°」。
     * <p>
     * 只借用现有的环绕接口推进初始 yaw，不改动相机自身的任何行为；
     * 灵敏度与鼠标拖动环绕一致（0.6 度/像素），所以这里按像素量换算。
     */
    private static final double INITIAL_ORBIT_DEGREES = -90.0;
    private static final double ORBIT_DEGREES_PER_PIXEL = 0.6;

    private final Screen parent;
    private final AbstractTransformBlockEntity entity;
    private final CollisionVoxelGrid grid;
    private final CollisionPicker picker;
    private final CollisionViewportRenderer viewport;
    private final UndoHistory<long[]> history = new UndoHistory<>(UNDO_LIMIT);

    private final List<AbstractWidget> panels = new ArrayList<>();

    private boolean showGrid = true;
    private boolean showOriginalShape = true;
    private Button undoButton;
    private Button redoButton;

    /** 合并后的盒子数：重建 VoxelShape 有开销，只在数据变化后重算。 */
    private int cachedBoxCount;
    private boolean boxesDirty = true;

    /** 当前笔刷形状。 */
    private CollisionBrush brush = CollisionBrush.PIN;

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

    public CollisionEditorScreen(Screen parent, AbstractTransformBlockEntity entity) {
        super(Component.translatable("gui.yuushya.collision.title"));
        this.parent = parent;
        this.entity = entity;
        this.grid = new CollisionVoxelGrid();
        this.grid.fromShape(entity.getEffectiveShape());
        this.picker = new CollisionPicker(this.grid);
        this.viewport = new CollisionViewportRenderer(this.grid, entity, this.camera);
        this.camera.orbit(INITIAL_ORBIT_DEGREES / ORBIT_DEGREES_PER_PIXEL, 0.0);
    }

    // ---- 初始化 ----

    @Override
    protected void initScreen() {
        rebuildPanels();
    }

    /** 左栏工具 + 右栏选项 + 顶栏应用/取消。 */
    private void rebuildPanels() {
        clearManagedWidgets(this.panels);

        ModelingLayout.Rect left = this.layout.elements;
        int leftX = left.x() + 4;
        int leftWidth = Math.max(30, left.width() - 8);
        int y = left.y() + TITLE_HEIGHT + 2;
        addManagedWidget(this.panels, Button.builder(Component.translatable("gui.yuushya.collision.tool.fill"), btn -> fillAll())
                .bounds(leftX, y, leftWidth, ROW - 2).build());
        addManagedWidget(this.panels, Button.builder(Component.translatable("gui.yuushya.collision.tool.clear"), btn -> clearAll())
                .bounds(leftX, y + ROW, leftWidth, ROW - 2).build());
        this.undoButton = Button.builder(Component.translatable("gui.yuushya.collision.tool.undo"),
                btn -> undo()).bounds(leftX, y + ROW * 2, leftWidth, ROW - 2).build();
        addManagedWidget(this.panels, this.undoButton);
        this.redoButton = Button.builder(Component.translatable("gui.yuushya.collision.tool.redo"),
                btn -> redo()).bounds(leftX, y + ROW * 3, leftWidth, ROW - 2).build();
        addManagedWidget(this.panels, this.redoButton);
        addManagedWidget(this.panels, CycleButton.builder(CollisionBrush::label)
                .withValues(CollisionBrush.values())
                .withInitialValue(this.brush)
                .withTooltip(mode -> Tooltip.create(Component.translatable("gui.yuushya.collision.brush.tip")))
                .create(leftX, y + ROW * 4, leftWidth, ROW - 2,
                        Component.translatable("gui.yuushya.collision.brush"),
                        (button, mode) -> this.brush = mode));

        ModelingLayout.Rect right = this.layout.right;
        int rightX = right.x() + 4;
        int rightWidth = Math.max(40, right.width() - 8);
        int rightY = right.y() + TITLE_HEIGHT + 2;
        addManagedWidget(this.panels, Checkbox.builder(Component.translatable("gui.yuushya.collision.option.grid"), this.font)
                .pos(rightX, rightY).maxWidth(rightWidth).selected(this.showGrid)
                .onValueChange((box, value) -> this.showGrid = value)
                .build());
        addManagedWidget(this.panels, Checkbox.builder(Component.translatable("gui.yuushya.collision.option.original"), this.font)
                .pos(rightX, rightY + ROW).maxWidth(rightWidth).selected(this.showOriginalShape)
                .onValueChange((box, value) -> this.showOriginalShape = value)
                .build());

        ModelingLayout.Rect header = this.layout.header;
        int cancelWidth = 40;
        int applyWidth = 40;
        int headerY = header.y() + (header.height() - BUTTON_HEIGHT) / 2;
        addManagedWidget(this.panels, Button.builder(Component.translatable("gui.yuushya.collision.apply"), btn -> apply())
                .bounds(header.right() - 4 - cancelWidth - 4 - applyWidth, headerY, applyWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.yuushya.collision.apply.tip")))
                .build());
        addManagedWidget(this.panels, Button.builder(Component.translatable("gui.yuushya.collision.cancel"), btn -> onClose())
                .bounds(header.right() - 4 - cancelWidth, headerY, cancelWidth, BUTTON_HEIGHT)
                .build());

        refreshUndoButtons();
    }

    private void refreshUndoButtons() {
        if (this.undoButton != null) {
            this.undoButton.active = this.history.canUndo();
        }
        if (this.redoButton != null) {
            this.redoButton.active = this.history.canRedo();
        }
    }

    // ---- 编辑动作 ----

    /** 改动前先压一份快照，撤销才有得回。 */
    private void pushUndo() {
        pushUndo(this.grid.snapshot());
    }

    /** 压入一份已经拍好的快照（一笔拖动只在第一次真正改动时压一次）。 */
    private void pushUndo(long[] snapshot) {
        this.history.push(snapshot);
        refreshUndoButtons();
    }

    private void undo() {
        long[] previous = this.history.undo(this.grid.snapshot());
        if (previous == null) {
            return;
        }
        this.grid.restore(previous);
        this.boxesDirty = true;
        refreshUndoButtons();
    }

    private void redo() {
        long[] next = this.history.redo(this.grid.snapshot());
        if (next == null) {
            return;
        }
        this.grid.restore(next);
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

    /** 把当前笔刷的范围整体写成 {@code filled}；没变化时不压撤销。 */
    private void paintBrush(int[] anchor, boolean filled) {
        int[] range = this.brush.range(anchor, anchor[3]);
        if (!this.grid.needsFill(range, filled)) {
            return;
        }
        pushUndo();
        if (this.grid.fill(range, filled)) {
            this.boxesDirty = true;
        }
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
        int[] anchor = this.picker.pick(mouseX, mouseY, this.brush, place, this.camera, this.layout.viewport);
        if (anchor != null) {
            this.rectEnd = anchor;
        }
    }

    /** 两个角围成的格子范围（闭区间）；没有方框时返回 {@code null}。 */
    private int[] rectRange() {
        if (this.rectStart == null || this.rectEnd == null) {
            return null;
        }
        return CollisionVoxelGrid.rangeBetween(this.rectStart, this.rectEnd);
    }

    /** 松开方框时提交：整框填充 / 清除，只算一步撤销。 */
    private void commitRect(boolean place) {
        int[] range = rectRange();
        if (range == null || !this.grid.needsFill(range, place)) {
            return;
        }
        pushUndo(this.rectSnapshot);
        if (this.grid.fill(range, place)) {
            this.boxesDirty = true;
        }
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
        this.viewport.render(guiGraphics, this.layout.viewport, viewState());
        renderAxisIndicator(guiGraphics, mouseX, mouseY);
        ModelingChrome.render(guiGraphics, this.font, this.layout, this.entity.getBlockState().getBlock().getName(),
                null, "gui.yuushya.collision.tools", "gui.yuushya.collision.options");
        renderCounters(guiGraphics);
        renderHint(guiGraphics);
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /** 刷新悬停预览；拖方框时不做「粘滞」，预览直接跟随光标。 */
    private void updateHover(double mouseX, double mouseY) {
        this.picker.update(mouseX, mouseY, this.brush, this.rectButton < 0, this.camera, this.layout.viewport);
    }

    /** 本帧要画的悬停范围：方框拖动期间用整框，其余用解析出的笔刷体积。 */
    private CollisionViewportRenderer.State viewState() {
        int[] placeRange;
        int[] eraseRange;
        if (this.rectButton >= 0) {
            int[] range = rectRange();
            boolean place = this.rectButton == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            placeRange = place ? range : null;
            eraseRange = place ? null : range;
        } else {
            int[] place = this.picker.placeTarget();
            int[] erase = this.picker.eraseTarget();
            placeRange = place == null ? null : this.brush.range(place, place[3]);
            eraseRange = erase == null ? null : this.brush.range(erase, erase[3]);
        }
        return new CollisionViewportRenderer.State(this.showGrid, this.showOriginalShape, placeRange, eraseRange);
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
        } else {
            int[] place = this.picker.placeTarget();
            if (place != null) {
                boolean empty = !this.grid.get(place[0], place[1], place[2]);
                hint = Component.translatable(empty
                        ? "gui.yuushya.collision.hover.fill"
                        : "gui.yuushya.collision.hover.erase").getString();
            }
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
            anchor = left ? this.picker.placeTarget() : this.picker.eraseTarget();
            if (anchor == null) {
                anchor = this.picker.pick(mouseX, mouseY, this.brush, left, this.camera, this.layout.viewport);
            }
        }

        // 方框笔刷：按住拖动划范围，松开才提交，期间不操作视角
        if (paintButton && anchor != null && this.brush == CollisionBrush.RECT) {
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