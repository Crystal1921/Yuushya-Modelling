package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.gui.modeling.target.ModelingChannel;
import com.yuushya.modelling.gui.modeling.target.ModelingTarget;
import com.yuushya.modelling.gui.modeling.target.TransformRow;
import com.yuushya.modelling.gui.widget.NumberField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 建模界面右下角的「变换」面板。
 * <p>
 * Blender / Blockbench 式的紧凑排版：每个分组只占一行，X / Y / Z 三个数字框并排，
 * 直接拖动即可改值。行定义来自 {@link ModelingTarget#rows()}，因此新增一种方块类型
 * 只需要在 target 里多返回一行，这里不改代码。
 */
public final class ModelingProperties {

    private static final int ROW_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 12;
    private static final int LABEL_WIDTH = 46;
    private static final int GAP = 3;

    private final ModelingTarget target;
    private final BiConsumer<ModelingChannel, Double> onChange;
    private final List<NumberField> fields = new ArrayList<>();
    private final List<ModelingChannel> fieldChannels = new ArrayList<>();
    private final Map<Integer, Component> rowLabels = new LinkedHashMap<>();
    /** 缩放行上方的「等比」复选框；没有缩放行时为 {@code null}。 */
    private Checkbox uniformScale;
    /** 「等比」复选框的当前状态，拖动数字框时以此决定是否同步三轴。 */
    private boolean uniformScaleOn;
    private int slot;

    public ModelingProperties(ModelingTarget target, BiConsumer<ModelingChannel, Double> onChange) {
        this.target = target;
        this.onChange = onChange;
    }

    /** 在给定区域内按 {@link ModelingTarget#rows()} 排布。 */
    public void layout(ModelingLayout.Rect rect, int slot, Font font) {
        this.fields.clear();
        this.fieldChannels.clear();
        this.rowLabels.clear();
        this.uniformScale = null;
        this.slot = slot;

        List<TransformRow> all = this.target.rows();
        int maxRows = Math.max(0, (rect.height() - TITLE_HEIGHT) / ROW_HEIGHT);
        for (int row = 0; row < all.size() && row < maxRows; row++) {
            TransformRow transformRow = all.get(row);
            this.rowLabels.put(row, transformRow.label());
            if (transformRow instanceof TransformRow.Vector vector) {
                // 缩放行的标签后面紧跟一个无文字的「等比」复选框，字段整体给它让位
                int startX = isScaleRow(vector)
                        ? addUniformScaleCheckbox(rect, row, font, slot, vector.label())
                        : rect.x() + LABEL_WIDTH;
                addVectorRow(rect, row, font, vector, startX);
            } else if (transformRow instanceof TransformRow.Single single) {
                addSingleRow(rect, row, font, single);
            }
        }
    }

    private static boolean isScaleRow(TransformRow.Vector row) {
        return row.x() == ModelingChannel.SCALE_X;
    }

    /**
     * 在缩放行的标签后面摆一个只有方框、没有文字的复选框。
     *
     * @return 数字框的起始 x，至少从 {@link #LABEL_WIDTH} 处开始
     */
    private int addUniformScaleCheckbox(ModelingLayout.Rect rect, int row, Font font, int slot, Component label) {
        this.uniformScaleOn = scaleAxesEqual(slot);
        int boxSize = Checkbox.getBoxSize(font);
        int x = rect.x() + 5 + font.width(label) + 2;
        int y = rowY(rect, row) + (ROW_HEIGHT - boxSize) / 2;
        this.uniformScale = Checkbox.builder(Component.empty(), font)
                .pos(x, y)
                .maxWidth(boxSize)
                .selected(this.uniformScaleOn)
                .tooltip(Tooltip.create(Component.translatable("gui.yuushya.modeling.uniformScale.tip")))
                .onValueChange((checkbox, value) -> this.uniformScaleOn = value)
                .build();
        return Math.max(rect.x() + LABEL_WIDTH, x + boxSize + GAP);
    }

    /** 当前元素三轴缩放是否一致（决定复选框的初始状态）。 */
    private boolean scaleAxesEqual(int slot) {
        double x = this.target.get(slot, ModelingChannel.SCALE_X);
        double y = this.target.get(slot, ModelingChannel.SCALE_Y);
        double z = this.target.get(slot, ModelingChannel.SCALE_Z);
        return Math.abs(x - y) < 1.0E-4 && Math.abs(y - z) < 1.0E-4;
    }

    /** 勾选「等比」后，改任意一轴都要同步三轴。 */
    public boolean isUniformScale() {
        return this.uniformScaleOn;
    }

    private int rowY(ModelingLayout.Rect rect, int row) {
        return rect.y() + TITLE_HEIGHT + row * ROW_HEIGHT;
    }

    private void addVectorRow(ModelingLayout.Rect rect, int row, Font font, TransformRow.Vector vector, int startX) {
        int y = rowY(rect, row);
        int totalWidth = rect.right() - 6 - startX;
        int fieldWidth = Math.max(20, (totalWidth - GAP * 2) / 3);
        ModelingChannel[] channels = {vector.x(), vector.y(), vector.z()};
        for (int axis = 0; axis < 3; axis++) {
            addField(channels[axis], font, startX + axis * (fieldWidth + GAP), y, fieldWidth,
                    vector.step(), vector.decimals(), vector.min(), vector.max());
        }
    }

    private void addSingleRow(ModelingLayout.Rect rect, int row, Font font, TransformRow.Single single) {
        int y = rowY(rect, row);
        int startX = rect.x() + LABEL_WIDTH;
        int width = single.width() > 0 ? single.width() : rect.right() - 6 - startX;
        addField(single.channel(), font, startX, y, width, single.step(), single.decimals(),
                single.min(), single.max());
    }

    private void addField(ModelingChannel channel, Font font, int x, int y, int width,
                          double step, int decimals, double min, double max) {
        NumberField field = new NumberField(font, x, y, width, ROW_HEIGHT - 2, step, decimals,
                min, max, this.target.get(this.slot, channel),
                value -> this.onChange.accept(channel, value));
        this.fields.add(field);
        this.fieldChannels.add(channel);
    }

    public List<AbstractWidget> widgets() {
        List<AbstractWidget> widgets = new ArrayList<>(this.fields);
        if (this.uniformScale != null) {
            widgets.add(this.uniformScale);
        }
        return widgets;
    }

    /** 收起面板（例如切到颜色编辑器）时清空行定义与控件。 */
    public void clear() {
        this.fields.clear();
        this.fieldChannels.clear();
        this.rowLabels.clear();
        this.uniformScale = null;
        this.uniformScaleOn = false;
    }

    /** 切换元素后把字段刷到新数据，「等比」复选框也按新元素的三轴重新判断。 */
    public void refresh(int slot) {
        this.slot = slot;
        for (int i = 0; i < this.fields.size(); i++) {
            this.fields.get(i).setValue(this.target.get(slot, this.fieldChannels.get(i)));
        }
        if (this.uniformScale != null && this.uniformScale.selected() != scaleAxesEqual(slot)) {
            // Checkbox 没有 setSelected，走一次和点击相同的流程，onValueChange 会同步 uniformScaleOn
            this.uniformScale.onPress();
        }
    }

    /** 外部改值后同步字段显示。 */
    public void sync(ModelingChannel channel, double value) {
        for (int i = 0; i < this.fields.size(); i++) {
            if (this.fieldChannels.get(i) == channel) {
                this.fields.get(i).setValue(value);
            }
        }
    }

    public Map<Integer, Component> labels() {
        return this.rowLabels;
    }

    public boolean isEditing() {
        for (NumberField field : this.fields) {
            if (field.isEditing()) {
                return true;
            }
        }
        return false;
    }

    public void commitAll() {
        for (NumberField field : this.fields) {
            field.commitEdit();
        }
    }
}