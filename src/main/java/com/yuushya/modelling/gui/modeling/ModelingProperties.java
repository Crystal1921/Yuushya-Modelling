package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.gui.modeling.target.ModelingChannel;
import com.yuushya.modelling.gui.modeling.target.ModelingTarget;
import com.yuushya.modelling.gui.modeling.target.TransformRow;
import com.yuushya.modelling.gui.widget.NumberField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
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
        this.slot = slot;

        List<TransformRow> all = this.target.rows();
        int maxRows = Math.max(0, (rect.height() - TITLE_HEIGHT) / ROW_HEIGHT);
        for (int i = 0; i < all.size() && i < maxRows; i++) {
            TransformRow row = all.get(i);
            this.rowLabels.put(i, row.label());
            if (row instanceof TransformRow.Vector vector) {
                addVectorRow(rect, i, font, vector);
            } else if (row instanceof TransformRow.Single single) {
                addSingleRow(rect, i, font, single);
            }
        }
    }

    private int rowY(ModelingLayout.Rect rect, int row) {
        return rect.y() + TITLE_HEIGHT + row * ROW_HEIGHT;
    }

    private void addVectorRow(ModelingLayout.Rect rect, int row, Font font, TransformRow.Vector vector) {
        int y = rowY(rect, row);
        int startX = rect.x() + LABEL_WIDTH;
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
        return new ArrayList<>(this.fields);
    }

    /** 切换元素后把字段刷到新数据。 */
    public void refresh(int slot) {
        this.slot = slot;
        for (int i = 0; i < this.fields.size(); i++) {
            this.fields.get(i).setValue(this.target.get(slot, this.fieldChannels.get(i)));
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