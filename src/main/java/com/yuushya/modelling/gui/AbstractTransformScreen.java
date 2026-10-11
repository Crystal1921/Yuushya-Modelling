package com.yuushya.modelling.gui;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.gui.showblock.EditScreen;
import com.yuushya.modelling.gui.transform.AxisDragController;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.validate.ValidateRange;
import com.yuushya.modelling.gui.widget.AbstractColorScreen;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.gui.widget.TransformComponent;
import com.yuushya.modelling.utils.CameraProjection;
import com.yuushya.modelling.utils.ClientMethod;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * 变换编辑界面的通用基类。
 * <p>
 * ShowBlockScreen、ItemBlockScreen、TextBlockScreen 共用同一套布局常量、滑块面板管理、
 * 待发送数据暂存与槽位切换流程，这些内容统一收敛到这里；各界面只负责对接自己的
 * 变换枚举、网络包以及特有的面板控件。
 *
 * @param <T> 该界面使用的变换类型枚举
 * @param <E> 该界面编辑的方块实体类型
 */
public abstract class AbstractTransformScreen<T, E extends AbstractTransformBlockEntity> extends AbstractColorScreen {
    public static final int PER_HEIGHT = 20;
    public static final int SMALL_BUTTON_WIDTH = 10;
    protected static final int TOP = 10;
    protected static final int RIGHT_COLUMN_X = 2;
    protected static final int RIGHT_BAR_WIDTH = PER_HEIGHT;
    protected static final int RIGHT_LIST_WIDTH = 40;
    protected static final int RIGHT_LIST_PER_HEIGHT = 45;
    protected static final int RIGHT_LIST_TOP = TOP + PER_HEIGHT + 5;
    protected static final int RIGHT_LIST_HEIGHT = 3 * RIGHT_LIST_PER_HEIGHT + 2;
    protected static final int RIGHT_LIST_BOTTOM = RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT;
    protected static final int RIGHT_STATE_PANEL_Y = RIGHT_LIST_BOTTOM + 5;
    protected static final int RIGHT_STATE_INFORM_X = RIGHT_COLUMN_X + RIGHT_LIST_WIDTH + 3;

    protected final E blockEntity;
    /**
     * 尚未下发到服务端的变换改动，切换槽位或关闭界面时统一冲刷。
     */
    protected final Map<T, Double> storage = new HashMap<>();
    /**
     * 左侧滑块面板，按创建顺序保存。
     */
    protected final Map<T, TransformComponent<T>> panel = new LinkedHashMap<>();
    public EditBox colorEditBox;
    public ColorWidget colorWidget;
    protected int slot;

    /** 三轴的平移 / 旋转 / 缩放分量，由 addTransformRows / addUniformScaleRow 登记，供拖拽换算使用。 */
    private List<T> posTypes;
    private List<T> rotTypes;
    private List<T> scaleTypes;
    /** 提示中展示的轴。 */
    private int displayAxis = 0;
    /** 世界区域提示的显示截止时间。 */
    private long hintUntil = 0L;
    /** 世界区域拖拽会话。 */
    private final AxisDragController dragController = new AxisDragController(new AxisDragController.Host() {
        @Override
        public double value(int axis, boolean rotation) {
            return extract(typeFor(axis, rotation), slot);
        }

        @Override
        public void apply(int axis, boolean rotation, double value) {
            applyDraggedValue(axis, rotation, value);
        }

        @Override
        public void highlight(int axis) {
            highlightAxis(axis);
        }

        @Override
        public double axisScale(int axis) {
            return axisScaleOf(axis);
        }

        @Override
        public float[] axisOriginWorld() {
            return gizmoOriginWorld();
        }

        @Override
        public float[] axisDirection(int axis) {
            return axisDirectionWorld(axis);
        }
    });

    protected AbstractTransformScreen(Component title, E blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
    }

    // i \in [1,...]
    protected static int top(int i, int offset) {
        return TOP + PER_HEIGHT + 10 + PER_HEIGHT * i + offset;
    }

    protected int leftColumnX() {
        return this.width / 4 * 3 + 10;
    }

    protected int leftColumnWidth() {
        return this.width / 4 - 20;
    }

    /**
     * 读取指定槽位上该变换分量的当前值。
     */
    protected abstract double extract(T type, int slot);

    /**
     * 修改本地数据（不涉及网络）。
     */
    protected abstract void modify(T type, int slot, double number);

    /**
     * 将单个变换分量发送到服务端。
     */
    protected abstract void sendTransform(BlockPos pos, int slot, T type, double number);

    /**
     * 通知服务端本批次改动结束。
     */
    protected abstract void sendSuccess(BlockPos pos);

    /**
     * 该界面是否处于“编辑输入框”模式。
     */
    protected abstract boolean isEditMode();

    /**
     * 绘制左侧的槽位列表。
     */
    protected abstract void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /**
     * 绘制列表以外、滑块编辑框之前的补充信息（列表名称、属性等）。
     */
    protected void renderHeader(GuiGraphics guiGraphics) {
    }

    /**
     * 绘制滑块编辑框之后、{@code super.render} 之前的补充信息。
     */
    protected void renderFooter(GuiGraphics guiGraphics) {
    }

    /**
     * 获取（必要时创建）某个变换分量对应的滑块行。
     */
    protected TransformComponent<T> choose(T type) {
        return choose(type, TransformComponent.Style.SLIDER_ROW);
    }

    protected TransformComponent<T> choose(T type, TransformComponent.Style style) {
        return panel.computeIfAbsent(type, key -> new TransformComponent<>(key, style));
    }

    /**
     * 应用变化：暂存到 storage、修改本地数据并触发方块更新。
     */
    public void updateTransformDataClient(T type, Double number) {
        this.storage.put(type, number);
        modify(type, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    /**
     * 把 storage 中暂存的改动全部下发到服务端。
     */
    protected void flushPending() {
        for (Map.Entry<T, Double> entry : this.storage.entrySet()) {
            sendTransform(blockEntity.getBlockPos(), this.slot, entry.getKey(), entry.getValue());
        }
        this.storage.clear();
    }

    // ---- 左侧滑块行 ----

    private static final Direction.Axis[] AXES = {Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
    private static final String[] AXIS_NAMES = {"x", "y", "z"};
    private static final ChatFormatting[] AXIS_COLORS = {ChatFormatting.DARK_RED, ChatFormatting.GREEN, ChatFormatting.BLUE};

    /**
     * 高亮对应轴，并保持坐标轴常显。
     * <p>
     * 始终使用未旋转的三个轴：数据中平移沿朝向坐标系的轴、旋转绕该坐标系的轴，
     * 两者都是这个坐标系，因此画出来的轴线与拖拽方向始终一致。
     */
    private void highlightAxis(int axis) {
        this.blockEntity.setShowAxis(AXES[axis]);
        this.blockEntity.setShowPosAxis();
    }

    /**
     * 轴向上的“分量”文本，用于滑块与编辑框。
     */
    private static Component axisDecoration(String axisKeyPrefix, int axis) {
        return Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], "").withStyle(AXIS_COLORS[axis]);
    }

    /**
     * 构建 X/Y/Z 共 6 行平移与旋转滑块。
     */
    protected final void addTransformRows(List<T> posTypes, List<T> rotTypes, String captionKeyPrefix, String axisKeyPrefix,
                                          double boundX, double boundY, double boundZ) {
        this.posTypes = posTypes;
        this.rotTypes = rotTypes;
        double[] bounds = {boundX, boundY, boundZ};
        for (int axis = 0; axis < 3; axis++) {
            addPositionRow(posTypes.get(axis), axis, Component.translatable(captionKeyPrefix + "pos_text"), axisKeyPrefix, axis, bounds[axis]);
        }
        for (int axis = 0; axis < 3; axis++) {
            addRotationRow(rotTypes.get(axis), axis, Component.translatable(captionKeyPrefix + "rot_text"), axisKeyPrefix, 3 + axis);
        }
    }

    /**
     * 平移滑块：范围 ±bound，步长随范围自适应。
     */
    private void addPositionRow(T type, int axis, Component caption, String axisKeyPrefix, int row, double bound) {
        choose(type).sliderButton =
                LazyDoubleRange.buttonBuilder(caption,
                                () -> -bound,
                                () -> bound,
                                () -> getStep(bound),
                                (number) -> updateTransformDataClient(type, number))
                        .text((c, number) -> Component.empty().append(c).append(Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], String.format("%05.1f", number)).withStyle(AXIS_COLORS[axis])))
                        .step(choose(type).setStandardStep(0.0))
                        .onMouseOver((btn) -> highlightAxis(axis))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 0), leftColumnWidth(), PER_HEIGHT).build();
        choose(type).decoration(axisDecoration(axisKeyPrefix, axis));
    }

    /**
     * 旋转滑块：范围 0~360，步长 22.5。
     */
    private void addRotationRow(T type, int axis, Component caption, String axisKeyPrefix, int row) {
        choose(type).sliderButton =
                DoubleRange.buttonBuilder(caption, 0.0, 360.0, (number) -> updateTransformDataClient(type, number))
                        .text((c, number) -> Component.empty().append(c).append(Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], String.format("%05.1f", number)).withStyle(AXIS_COLORS[axis])))
                        .step(choose(type).setStandardStep(22.5))
                        .onMouseOver((btn) -> highlightAxis(axis))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 10), leftColumnWidth(), PER_HEIGHT).build();
        choose(type).decoration(axisDecoration(axisKeyPrefix, axis));
    }

    /**
     * 亮度滑块：范围 0~15，步长 1。
     */
    protected final void addBrightnessRow(T type, Component caption, int row) {
        choose(type).sliderButton =
                DoubleRange.buttonBuilder(caption, 0.0, 15.0, (number) -> updateTransformDataClient(type, number))
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(type).setStandardStep(1))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 30), leftColumnWidth(), PER_HEIGHT).build();
    }

    /**
     * 三轴等比缩放滑块：拖动时会同步三轴并刷新对应轴的平移滑块。
     */
    protected final void addUniformScaleRow(List<T> scaleTypes, List<T> posTypes, Component caption, String scaleTextKey, int row, double bound) {
        this.setScaleTypes(scaleTypes);
        T scaleX = scaleTypes.get(0);
        choose(scaleX).sliderButton =
                LazyDoubleRange.buttonBuilder(caption,
                                () -> -bound,
                                () -> bound,
                                (number) -> {
                                    // 防止输入 0 或浮点误差内的近似 0，回退为 1.0
                                    double value = Math.abs(number) <= 0.001 ? 1.0 : number;
                                    for (T scaleType : scaleTypes) {
                                        updateTransformDataClient(scaleType, value);
                                    }
                                    choose(scaleX).sliderButton.setValidatedValue(value);
                                    for (T posType : posTypes) {
                                        choose(posType).sliderButton.setValidatedValue(choose(posType).sliderButton.getValidatedValue());
                                    }
                                })
                        .text((c, number) -> Component.translatable(scaleTextKey, String.format("%05.1f", number)))
                        .step(choose(scaleX).setStandardStep(0.1))
                        .initial(extract(scaleX, slot))
                        .bounds(leftColumnX(), top(row, 20), leftColumnWidth(), PER_HEIGHT).build();
        choose(scaleX).decoration(Component.translatable(scaleTextKey, ""));
    }

    /**
     * 登记三个缩放分量。缩放行不通过 {@link #addUniformScaleRow} 构建的界面（如文本框界面）需要显式调用，
     * 否则拖拽换算会退化为按 1 倍缩放处理。
     */
    protected final void setScaleTypes(List<T> scaleTypes) {
        this.scaleTypes = scaleTypes;
    }

    /**
     * 单独一轴的尺寸滑块（自定义尺寸面板使用）。
     */
    protected final void addSizeRow(Map<T, TransformComponent<T>> sizePanel, T scaleType, T posType, int row, Component caption, String scaleTextKey, double bound) {
        TransformComponent<T> component = sizePanel.computeIfAbsent(scaleType, key -> new TransformComponent<>(key, TransformComponent.Style.SIZE_ROW));
        component.sliderButton =
                LazyDoubleRange.buttonBuilder(caption,
                                () -> -bound,
                                () -> bound,
                                (number) -> {
                                    // 防止输入 0 或浮点误差内的近似 0，回退为 1.0
                                    double value = Math.abs(number) <= 0.001 ? 1.0 : number;
                                    updateTransformDataClient(scaleType, value);
                                    component.editBox.setValue(String.valueOf(value));
                                    component.sliderButton.setValidatedValue(value);
                                    choose(posType).sliderButton.setValidatedValue(choose(posType).sliderButton.getValidatedValue());
                                })
                        .text((c, number) -> Component.translatable(scaleTextKey, String.format("%05.1f", number)))
                        .step(component.setStandardStep(0.1))
                        .initial(extract(scaleType, slot))
                        .bounds(leftColumnX(), top(row, 20), leftColumnWidth(), PER_HEIGHT).build();
    }

    /**
     * 形状按钮，三个界面完全一致。
     */
    protected final CycleButton<BlockShape> createShapeButton(T shapeType, BlockShape shape) {
        return CycleButton.builder(BlockShape::getSymbol)
                .displayOnlyValue()
                .withValues(BlockShape.values())
                .withInitialValue(shape)
                .create(leftColumnX() - 50, TOP, 40, PER_HEIGHT, Component.literal("shape"),
                        (button, selected) -> updateTransformDataClient(shapeType, (double) selected.ordinal()));
    }

    /**
     * 显示/隐藏切换按钮。
     */
    protected final CycleButton<Boolean> createShownButton(T shownType, int x) {
        return CycleButton.booleanBuilder(Component.literal("🕶"), Component.literal("👀"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(shownType, bl ? 1.0 : 0.0));
    }

    /**
     * 界面模式：滑块 / 微调 / 编辑，以及各界面特有的模式。
     */
    public interface ScreenMode extends StringRepresentable {
        Component getSymbol();

        Component getTooltip();
    }

    /**
     * 模式切换按钮，三个界面完全一致。
     */
    protected final <M extends Enum<M> & ScreenMode> CycleButton<M> createModeButton(M[] values, M initial, BiConsumer<CycleButton<M>, M> onChange) {
        return CycleButton.<M>builder(mode -> mode.getSymbol())
                .displayOnlyValue()
                .withValues(values)
                .withInitialValue(initial)
                .withTooltip((mode) -> Tooltip.create(mode.getTooltip()))
                .create(leftColumnX(), TOP, leftColumnWidth(), PER_HEIGHT, Component.literal("MODE"),
                        (button, mode) -> onChange.accept(button, mode));
    }

    /**
     * SLIDER / FINE_TUNE / EDIT 三种模式对滑块面板的统一处理。
     */
    protected final void applySliderMode(boolean fineTune, boolean sliderVisible) {
        for (TransformComponent<T> component : this.panel.values()) {
            if (fineTune) {
                component.setSliderFineTune();
            } else {
                component.setSliderStep();
            }
            component.triggerVisible(sliderVisible);
        }
    }

    // ---- 世界区域拖拽 ----

    /** 操作提示的持续时长。 */
    private static final long HINT_MILLIS = 4000L;

    /** 调试覆盖层开关（F9）。默认关闭：仅在排查拖拽几何时开启。 */
    private boolean debugOverlay = false;

    /** 手柄原点（方块局部坐标，1 单位 = 1 方块），与渲染器的 translateAfterScale + MIDDLE 一致。 */
    private float[] gizmoOrigin() {
        if (this.posTypes == null) {
            return null;
        }
        return new float[]{
                0.5F + (float) (extract(this.posTypes.get(0), slot) * axisScaleOf(0) / 16.0),
                0.5F + (float) (extract(this.posTypes.get(1), slot) * axisScaleOf(1) / 16.0),
                0.5F + (float) (extract(this.posTypes.get(2), slot) * axisScaleOf(2) / 16.0)};
    }

    /** 手柄原点的世界坐标。 */
    private float[] gizmoOriginWorld() {
        return toWorld(gizmoOrigin());
    }

    /**
     * 方块局部坐标 → 世界坐标，与渲染器的
     * {@code translate(0.5,0.5,0.5) · mulPose(facing) · translate(-0.5,-0.5,-0.5)} 完全一致。
     */
    private float[] toWorld(float[] local) {
        if (local == null) {
            return null;
        }
        Vector3f offset = new Vector3f(local[0] - 0.5F, local[1] - 0.5F, local[2] - 0.5F).rotate(facingRotation());
        BlockPos origin = this.blockEntity.getBlockPos();
        return new float[]{origin.getX() + 0.5F + offset.x, origin.getY() + 0.5F + offset.y, origin.getZ() + 0.5F + offset.z};
    }

    /** 该轴在世界坐标中的单位方向。 */
    private float[] axisDirectionWorld(int axis) {
        Vector3f direction = switch (axis) {
            case 0 -> new Vector3f(1.0F, 0.0F, 0.0F);
            case 1 -> new Vector3f(0.0F, 1.0F, 0.0F);
            default -> new Vector3f(0.0F, 0.0F, 1.0F);
        };
        direction.rotate(facingRotation());
        return new float[]{direction.x, direction.y, direction.z};
    }

    /** 方块朝向绕 Y 的旋转，与渲染器的 {@code Axis.YP.rotationDegrees(-facing.toYRot())} 一致。 */
    private Quaternionf facingRotation() {
        return Axis.YP.rotationDegrees(-this.blockEntity.getBlockState().getValue(HORIZONTAL_FACING).toYRot());
    }

    private double axisScaleOf(int axis) {
        return this.scaleTypes == null ? 1.0 : extract(this.scaleTypes.get(axis), slot);
    }

    private T typeFor(int axis, boolean rotation) {
        return (rotation ? this.rotTypes : this.posTypes).get(axis);
    }

    /** 平移/旋转面板都已登记时才允许拖拽。 */
    private boolean canDrag() {
        return this.posTypes != null && this.rotTypes != null;
    }

    /** 把拖拽算出的值写入数据与控件。 */
    private void applyDraggedValue(int axis, boolean rotation, double value) {
        if (!canDrag()) {
            return;
        }
        T type = typeFor(axis, rotation);
        TransformComponent<T> component = choose(type);
        if (component.sliderButton == null) {
            return;
        }
        ValidateRange<Double> range = component.sliderButton.getValidateRange();
        double min = range.minInclusive();
        double max = range.maxInclusive();
        double target = rotation ? wrapRotation(value, min, max) : Mth.clamp(value, min, max);
        if (target == extract(type, slot)) {
            return;
        }
        updateTransformDataClient(type, target);
        component.sliderButton.setValidatedValue(target);
        if (isEditMode()) {
            component.setEditBoxInitial();
        }
    }

    /** 旋转是循环量，越界时绕回而不是被夹住。 */
    private static double wrapRotation(double value, double min, double max) {
        double span = max - min;
        if (span <= 0.0) {
            return value;
        }
        double wrapped = (value - min) % span;
        if (wrapped < 0.0) {
            wrapped += span;
        }
        return min + wrapped;
    }

    /** 鼠标是否落在任意可见控件上，用于区分“世界区域”。 */
    private boolean isOverWidget(double mouseX, double mouseY) {
        for (GuiEventListener child : this.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dragController.isActive() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            dragController.cancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F9) {
            this.debugOverlay = !this.debugOverlay;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if (canDrag() && (left || right) && !isOverWidget(mouseX, mouseY)) {
            int axis = dragController.nearestAxis(mouseX, mouseY, this.width, this.height);
            if (axis >= 0 && dragController.begin(axis, right, mouseX, mouseY, this.width, this.height)) {
                this.displayAxis = axis;
                this.hintUntil = System.currentTimeMillis() + HINT_MILLIS;
                highlightAxis(axis);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (!canDrag() || dragController.isActive() || isOverWidget(mouseX, mouseY)) {
            return;
        }
        int axis = dragController.nearestAxis(mouseX, mouseY, this.width, this.height);
        if (axis >= 0) {
            this.displayAxis = axis;
            this.hintUntil = System.currentTimeMillis() + HINT_MILLIS;
            highlightAxis(axis);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragController.isActive()) {
            dragController.update(mouseX, mouseY, hasShiftDown());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean dragging = dragController.isActive();
        dragController.end();
        super.mouseReleased(mouseX, mouseY, button);
        return dragging;
    }

    /**
     * 调试用（F9 开关）：把换算出的三条轴线段画在 GUI 上——它们应当与世界渲染器画出的三色轴线完全重合；
     * 同时输出原点、端点像素坐标与鼠标到各线的距离。
     */
    private void renderDebugOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.debugOverlay || !canDrag()) {
            return;
        }
        AxisDragController.DebugSnapshot snapshot = dragController.debugSnapshot(mouseX, mouseY, this.width, this.height);
        int left = 4;
        if (!snapshot.projectionUsable()) {
            guiGraphics.drawString(this.font, "gizmo: projection stale", left, left, 0xFFFF5555, true);
            return;
        }
        float[] originLocal = gizmoOrigin();
        float[] originWorld = toWorld(originLocal);
        if (originLocal != null && originWorld != null) {
            guiGraphics.drawString(this.font, String.format("local %.3f %.3f %.3f", originLocal[0], originLocal[1], originLocal[2]), left, left, 0xFFFFFFFF, true);
            guiGraphics.drawString(this.font, String.format("world %.3f %.3f %.3f", originWorld[0], originWorld[1], originWorld[2]), left, left + 12 * 5, 0xFFFFFFFF, true);
        }
        guiGraphics.drawString(this.font, String.format("proj %dms  overWidget %b", CameraProjection.projectionAgeMillis(), isOverWidget(mouseX, mouseY)),
                left, left + 12 * 6, 0xFFBBBBBB, true);
        guiGraphics.drawString(this.font, cameraCheck(), left, left + 12 * 7, 0xFFBBBBBB, true);
        // 独立验证：本方块的世界包围盒应当与世界里的方块严丝合缝
        drawDebugBox(guiGraphics, this.blockEntity.getBlockPos(), 0xFF33E0E0);
        if (snapshot.originPixels() != null) {
            int x = Math.round(snapshot.originPixels()[0]);
            int y = Math.round(snapshot.originPixels()[1]);
            guiGraphics.fill(x - 4, y, x + 5, y + 1, 0xFFFFFFFF);
            guiGraphics.fill(x, y - 4, x + 1, y + 5, 0xFFFFFFFF);
        }
        float[][] segments = snapshot.segments();
        if (segments == null) {
            guiGraphics.drawString(this.font, "gizmo: segments unavailable", left, left + 12, 0xFFFF5555, true);
            return;
        }
        int[] colors = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF};
        for (int i = 0; i < 3; i++) {
            float[] s = segments[i];
            drawDebugLine(guiGraphics, s[0], s[1], s[2], s[3], colors[i]);
            double distance = snapshot.distances() == null ? -1.0 : snapshot.distances()[i];
            double length = Math.hypot(s[2] - s[0], s[3] - s[1]);
            guiGraphics.drawString(this.font,
                    String.format("%c (%.0f,%.0f)-(%.0f,%.0f) len=%.0f d=%.1f", "XYZ".charAt(i), s[0], s[1], s[2], s[3], length, distance),
                    left, left + 12 * (i + 1), colors[i], true);
        }
        int picked = dragController.nearestAxis(mouseX, mouseY, this.width, this.height);
        guiGraphics.drawString(this.font,
                "picked " + (picked < 0 ? "-" : String.valueOf("XYZ".charAt(picked))),
                left, left + 12 * 4, 0xFFFFFF55, true);
    }

    /** 世界坐标 → GUI 像素；投影不可用或点在相机后方时返回 {@code null}。 */
    private float[] worldToScreen(double x, double y, double z) {
        Vector3f ndc = CameraProjection.toNdc(x, y, z);
        if (ndc == null) {
            return null;
        }
        return new float[]{(ndc.x * 0.5F + 0.5F) * this.width, (1.0F - (ndc.y * 0.5F + 0.5F)) * this.height};
    }

    /** 调试用：把指定方块的世界包围盒画成线框。 */
    private void drawDebugBox(GuiGraphics guiGraphics, BlockPos pos, int color) {
        float[][] corners = new float[8][];
        int[] xs = {0, 1, 1, 0, 0, 1, 1, 0};
        int[] ys = {0, 0, 0, 0, 1, 1, 1, 1};
        int[] zs = {0, 0, 1, 1, 0, 0, 1, 1};
        for (int i = 0; i < corners.length; i++) {
            corners[i] = worldToScreen(pos.getX() + xs[i], pos.getY() + ys[i], pos.getZ() + zs[i]);
            if (corners[i] == null) {
                return;
            }
        }
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] edge : edges) {
            drawDebugLine(guiGraphics, corners[edge[0]][0], corners[edge[0]][1], corners[edge[1]][0], corners[edge[1]][1], color);
        }
    }

    /** 自检：相机正前方 3 方块处的点必然落在屏幕正中，用来验证 MVP 链（调试用）。 */
    private String cameraCheck() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f forward = camera.rotation().transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f ndc = CameraProjection.toNdc(camPos.x + forward.x * 3.0, camPos.y + forward.y * 3.0, camPos.z + forward.z * 3.0);
        String eye = String.format("eye(%.2f,%.2f,%.2f) yaw %.0f pitch %.0f", camPos.x, camPos.y, camPos.z, camera.getYRot(), camera.getXRot());
        if (ndc == null) {
            return eye + " ahead=null";
        }
        return String.format("%s ahead((%.0f,%.0f) want (%.0f,%.0f))", eye,
                (ndc.x * 0.5F + 0.5F) * this.width, (1.0F - (ndc.y * 0.5F + 0.5F)) * this.height,
                this.width / 2.0, this.height / 2.0);
    }

    /** GUI 空间没有直接的线段绘制接口，用一串小方块近似。 */
    private static void drawDebugLine(GuiGraphics guiGraphics, float x0, float y0, float x1, float y1, int color) {
        int steps = (int) Math.min(600.0, Math.max(1.0, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)))));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int x = Math.round(x0 + (x1 - x0) * t);
            int y = Math.round(y0 + (y1 - y0) * t);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    /** 绘制世界区域拖拽的操作提示。 */
    private void renderDragHint(GuiGraphics guiGraphics) {
        if (!canDrag() || (!dragController.isActive() && System.currentTimeMillis() > hintUntil)) {
            return;
        }
        Component line = Component.translatable("gui.yuushya.transformDrag.hint",
                Component.literal(String.valueOf("XYZ".charAt(displayAxis))).withStyle(AXIS_COLORS[displayAxis]));
        int x = (this.width - this.font.width(line)) / 2;
        int y = this.height - this.font.lineHeight - 6;
        guiGraphics.fill(x - 4, y - 3, x + this.font.width(line) + 4, y + this.font.lineHeight + 2, 0x80000000);
        guiGraphics.drawString(this.font, line, x, y, 0xFFFFFFFF, false);
    }

    /**
     * 统一注册面板中的滑块及其附属按钮。
     */
    protected final void registerPanelWidgets() {
        for (TransformComponent<T> component : this.panel.values()) {
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
        }
    }

    @Override
    public EditBox getColorEditBox() {
        return this.colorEditBox;
    }

    /**
     * 切换槽位：冲刷暂存数据、切换槽位并刷新滑块初值。
     */
    public void setSlot(int slot) {
        dragController.end();
        flushPending();
        flushSlotExtra();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (TransformComponent<T> component : this.panel.values()) {
            component.setSliderInitial(extract(component.type, slot));
        }
        onSlotChanged();
    }

    /**
     * 切换槽位时额外需要下发到服务端的数据（物品、文本行等），默认无。
     */
    protected void flushSlotExtra() {
    }

    /**
     * 切换槽位后刷新与具体界面相关的控件状态。
     */
    protected abstract void onSlotChanged();

    @Override
    public void removed() {
        dragController.end();
        flushPending();
        flushOnRemoved();
        this.storage.clear();
        sendSuccess(blockEntity.getBlockPos());
    }

    /**
     * 关闭界面时额外需要下发到服务端的数据，默认无。
     */
    protected void flushOnRemoved() {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderList(guiGraphics, mouseX, mouseY, partialTick);
        renderHeader(guiGraphics);
        if (isEditMode()) {
            for (TransformComponent<T> component : this.panel.values()) {
                guiGraphics.drawString(this.font, component.editBox.getMessage(), component.editBox.getX() + component.editBox.getWidth() / 2, component.editBox.getY() + component.editBox.getHeight() / 3, 0x707070);
                component.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        renderFooter(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (canDrag()) {
            // 坐标轴常显：渲染端的可见性是按帧衰减的计数器，这里每帧续期
            this.blockEntity.setShowPosAxis();
        }
        renderDragHint(guiGraphics);
        renderDebugOverlay(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 分享 / 导入导出 ----

    /**
     * 分享与导入导出的差异点，由各界面实现。
     */
    public interface ShareController {
        /**
         * 序列化当前方块的全部数据。
         */
        String serialize();

        /**
         * 解析剪贴板文本并应用到当前方块，内容非法时抛出异常。
         */
        void load(String clipboard) throws Exception;

        /**
         * 把当前数据保存为指定名称的文件。
         */
        void save(String name) throws IOException;
    }

    protected abstract ShareController shareController();

    /**
     * 复制按钮：把当前数据序列化到剪贴板。
     */
    protected final Button createCopyButton(int x) {
        return Button.builder(Component.literal("\uD83D\uDCE4").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            ClientMethod.setClipboard(shareController().serialize());
                            toast(true, "copy_pass", Component.translatable("gui.showBlockScreen.workshop.share_hint"));
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.copy")))
                .bounds(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
    }

    /**
     * 粘贴按钮：读取剪贴板并应用到当前方块。
     */
    protected final Button createPasteButton(int x) {
        return Button.builder(Component.literal("\uD83D\uDCE5").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            try {
                                shareController().load(ClientMethod.getClipboard());
                                this.minecraft.getToasts().addToast(
                                        new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.paste_pass"), null));
                            } catch (Exception e) {
                                toast(false, "error", Component.literal(String.valueOf(e.getMessage())));
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.paste")))
                .bounds(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
    }

    /**
     * 保存按钮：把当前数据写入 modellings 目录下的文件。
     */
    protected final Button createSaveButton(int x) {
        return Button.builder(Component.literal("\uD83D\uDCBE").withStyle(ChatFormatting.BOLD),
                        (btn) -> this.minecraft.setScreen(new EditScreen(this,
                                Component.translatable("gui.showBlockScreen.workshop.save"),
                                Component.translatable("gui.showBlockScreen.workshop.save.tip"),
                                (string) -> {
                                    if (string != null) {
                                        try {
                                            shareController().save(string);
                                            toast(true, "save_pass", Component.translatable("gui.showBlockScreen.workshop.share_hint"));
                                        } catch (IOException e) {
                                            toast(false, "save_error", Component.literal(e.getMessage()));
                                        }
                                    }
                                    this.minecraft.setScreen(this);
                                },
                                (string) -> true)))
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.save")))
                .bounds(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
    }

    private void toast(boolean success, String key, Component detail) {
        this.minecraft.getToasts().addToast(success
                ? SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop." + key), detail)
                : SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop." + key), detail));
    }
}
