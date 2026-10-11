package com.yuushya.modelling.gui;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.gui.showblock.EditScreen;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.widget.AbstractColorScreen;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.gui.widget.TransformComponent;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;

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
    /** 尚未下发到服务端的变换改动，切换槽位或关闭界面时统一冲刷。 */
    protected final Map<T, Double> storage = new HashMap<>();
    /** 左侧滑块面板，按创建顺序保存。 */
    protected final Map<T, TransformComponent<T>> panel = new LinkedHashMap<>();
    public EditBox colorEditBox;
    public ColorWidget colorWidget;
    protected int slot;

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

    /** 读取指定槽位上该变换分量的当前值。 */
    protected abstract double extract(T type, int slot);

    /** 修改本地数据（不涉及网络）。 */
    protected abstract void modify(T type, int slot, double number);

    /** 将单个变换分量发送到服务端。 */
    protected abstract void sendTransform(BlockPos pos, int slot, T type, double number);

    /** 通知服务端本批次改动结束。 */
    protected abstract void sendSuccess(BlockPos pos);

    /** 该界面是否处于“编辑输入框”模式。 */
    protected abstract boolean isEditMode();

    /** 绘制左侧的槽位列表。 */
    protected abstract void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /** 绘制列表以外、滑块编辑框之前的补充信息（列表名称、属性等）。 */
    protected void renderHeader(GuiGraphics guiGraphics) {
    }

    /** 绘制滑块编辑框之后、{@code super.render} 之前的补充信息。 */
    protected void renderFooter(GuiGraphics guiGraphics) {
    }

    /** 获取（必要时创建）某个变换分量对应的滑块行。 */
    protected TransformComponent<T> choose(T type) {
        return choose(type, TransformComponent.Style.SLIDER_ROW);
    }

    protected TransformComponent<T> choose(T type, TransformComponent.Style style) {
        return panel.computeIfAbsent(type, key -> new TransformComponent<>(key, style));
    }

    /** 应用变化：暂存到 storage、修改本地数据并触发方块更新。 */
    public void updateTransformDataClient(T type, Double number) {
        this.storage.put(type, number);
        modify(type, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    /** 把 storage 中暂存的改动全部下发到服务端。 */
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

    /** 滑块悬停时高亮对应轴。 */
    private void highlightAxis(int axis, boolean rotation) {
        this.blockEntity.setShowAxis(AXES[axis]);
        if (rotation) {
            this.blockEntity.setShowRotAxis();
        } else {
            this.blockEntity.setShowPosAxis();
        }
    }

    /** 轴向上的“分量”文本，用于滑块与编辑框。 */
    private static Component axisDecoration(String axisKeyPrefix, int axis) {
        return Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], "").withStyle(AXIS_COLORS[axis]);
    }

    /** 构建 X/Y/Z 共 6 行平移与旋转滑块。 */
    protected final void addTransformRows(List<T> posTypes, List<T> rotTypes, String captionKeyPrefix, String axisKeyPrefix,
                                          double boundX, double boundY, double boundZ) {
        double[] bounds = {boundX, boundY, boundZ};
        for (int axis = 0; axis < 3; axis++) {
            addPositionRow(posTypes.get(axis), axis, Component.translatable(captionKeyPrefix + "pos_text"), axisKeyPrefix, axis, bounds[axis]);
        }
        for (int axis = 0; axis < 3; axis++) {
            addRotationRow(rotTypes.get(axis), axis, Component.translatable(captionKeyPrefix + "rot_text"), axisKeyPrefix, 3 + axis);
        }
    }

    /** 平移滑块：范围 ±bound，步长随范围自适应。 */
    private void addPositionRow(T type, int axis, Component caption, String axisKeyPrefix, int row, double bound) {
        choose(type).sliderButton =
                LazyDoubleRange.buttonBuilder(caption,
                                () -> -bound,
                                () -> bound,
                                () -> getStep(bound),
                                (number) -> updateTransformDataClient(type, number))
                        .text((c, number) -> Component.empty().append(c).append(Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], String.format("%05.1f", number)).withStyle(AXIS_COLORS[axis])))
                        .step(choose(type).setStandardStep(0.0))
                        .onMouseOver((btn) -> highlightAxis(axis, false))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 0), leftColumnWidth(), PER_HEIGHT).build();
        choose(type).decoration(axisDecoration(axisKeyPrefix, axis));
    }

    /** 旋转滑块：范围 0~360，步长 22.5。 */
    private void addRotationRow(T type, int axis, Component caption, String axisKeyPrefix, int row) {
        choose(type).sliderButton =
                DoubleRange.buttonBuilder(caption, 0.0, 360.0, (number) -> updateTransformDataClient(type, number))
                        .text((c, number) -> Component.empty().append(c).append(Component.translatable(axisKeyPrefix + AXIS_NAMES[axis], String.format("%05.1f", number)).withStyle(AXIS_COLORS[axis])))
                        .step(choose(type).setStandardStep(22.5))
                        .onMouseOver((btn) -> highlightAxis(axis, true))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 10), leftColumnWidth(), PER_HEIGHT).build();
        choose(type).decoration(axisDecoration(axisKeyPrefix, axis));
    }

    /** 亮度滑块：范围 0~15，步长 1。 */
    protected final void addBrightnessRow(T type, Component caption, int row) {
        choose(type).sliderButton =
                DoubleRange.buttonBuilder(caption, 0.0, 15.0, (number) -> updateTransformDataClient(type, number))
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(type).setStandardStep(1))
                        .initial(extract(type, slot))
                        .bounds(leftColumnX(), top(row, 30), leftColumnWidth(), PER_HEIGHT).build();
    }

    /** 三轴等比缩放滑块：拖动时会同步三轴并刷新对应轴的平移滑块。 */
    protected final void addUniformScaleRow(List<T> scaleTypes, List<T> posTypes, Component caption, String scaleTextKey, int row, double bound) {
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

    /** 单独一轴的尺寸滑块（自定义尺寸面板使用）。 */
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

    /** 形状按钮，三个界面完全一致。 */
    protected final CycleButton<BlockShape> createShapeButton(T shapeType, BlockShape shape) {
        return CycleButton.builder(BlockShape::getSymbol)
                .displayOnlyValue()
                .withValues(BlockShape.values())
                .withInitialValue(shape)
                .create(leftColumnX() - 50, TOP, 40, PER_HEIGHT, Component.literal("shape"),
                        (button, selected) -> updateTransformDataClient(shapeType, (double) selected.ordinal()));
    }

    /** 显示/隐藏切换按钮。 */
    protected final CycleButton<Boolean> createShownButton(T shownType, int x) {
        return CycleButton.booleanBuilder(Component.literal("🕶"), Component.literal("👀"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(shownType, bl ? 1.0 : 0.0));
    }

    /** 界面模式：滑块 / 微调 / 编辑，以及各界面特有的模式。 */
    public interface ScreenMode extends StringRepresentable {
        Component getSymbol();

        Component getTooltip();
    }

    /** 模式切换按钮，三个界面完全一致。 */
    protected final <M extends Enum<M> & ScreenMode> CycleButton<M> createModeButton(M[] values, M initial, BiConsumer<CycleButton<M>, M> onChange) {
        return CycleButton.<M>builder(mode -> mode.getSymbol())
                .displayOnlyValue()
                .withValues(values)
                .withInitialValue(initial)
                .withTooltip((mode) -> Tooltip.create(mode.getTooltip()))
                .create(leftColumnX(), TOP, leftColumnWidth(), PER_HEIGHT, Component.literal("MODE"),
                        (button, mode) -> onChange.accept(button, mode));
    }

    /** SLIDER / FINE_TUNE / EDIT 三种模式对滑块面板的统一处理。 */
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

    /** 统一注册面板中的滑块及其附属按钮。 */
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

    /** 切换槽位：冲刷暂存数据、切换槽位并刷新滑块初值。 */
    public void setSlot(int slot) {
        flushPending();
        flushSlotExtra();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (TransformComponent<T> component : this.panel.values()) {
            component.setSliderInitial(extract(component.type, slot));
        }
        onSlotChanged();
    }

    /** 切换槽位时额外需要下发到服务端的数据（物品、文本行等），默认无。 */
    protected void flushSlotExtra() {
    }

    /** 切换槽位后刷新与具体界面相关的控件状态。 */
    protected abstract void onSlotChanged();

    @Override
    public void removed() {
        flushPending();
        flushOnRemoved();
        this.storage.clear();
        sendSuccess(blockEntity.getBlockPos());
    }

    /** 关闭界面时额外需要下发到服务端的数据，默认无。 */
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
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 分享 / 导入导出 ----

    /** 分享与导入导出的差异点，由各界面实现。 */
    public interface ShareController {
        /** 序列化当前方块的全部数据。 */
        String serialize();

        /** 解析剪贴板文本并应用到当前方块，内容非法时抛出异常。 */
        void load(String clipboard) throws Exception;

        /** 把当前数据保存为指定名称的文件。 */
        void save(String name) throws IOException;
    }

    protected abstract ShareController shareController();

    /** 复制按钮：把当前数据序列化到剪贴板。 */
    protected final Button createCopyButton(int x) {
        return Button.builder(Component.literal("\uD83D\uDCE4").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            ClientMethod.setClipboard(shareController().serialize());
                            toast(true, "copy_pass", Component.translatable("gui.showBlockScreen.workshop.share_hint"));
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.copy")))
                .bounds(x, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
    }

    /** 粘贴按钮：读取剪贴板并应用到当前方块。 */
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

    /** 保存按钮：把当前数据写入 modellings 目录下的文件。 */
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
