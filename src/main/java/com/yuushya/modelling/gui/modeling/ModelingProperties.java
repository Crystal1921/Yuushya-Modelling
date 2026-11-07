package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformType;
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
 * 直接拖动即可改值。相比旧的"滑块 + 加减按钮 + 输入框"，横向占用减少约 2/3，
 * 因此 8 个分量只需要 4 行就能放完。
 */
public final class ModelingProperties {

    private static final int ROW_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 12;
    private static final int LABEL_WIDTH = 46;
    private static final int GAP = 3;

    private static final TransformType[] POS_TYPES = {TransformType.POS_X, TransformType.POS_Y, TransformType.POS_Z};
    private static final TransformType[] ROT_TYPES = {TransformType.ROT_X, TransformType.ROT_Y, TransformType.ROT_Z};
    private static final TransformType[] SCALE_TYPES = {TransformType.SCALE_X, TransformType.SCALE_Y, TransformType.SCALE_Z};

    private final ShowBlockEntity blockEntity;
    private final BiConsumer<TransformType, Double> onChange;
    private final List<NumberField> fields = new ArrayList<>();
    private final List<TransformType> fieldTypes = new ArrayList<>();

    public ModelingProperties(ShowBlockEntity blockEntity, BiConsumer<TransformType, Double> onChange) {
        this.blockEntity = blockEntity;
        this.onChange = onChange;
    }

    /** 在给定区域内排布四组：位移 / 旋转 / 缩放 / 亮度。 */
    public void layout(ModelingLayout.Rect rect, int slot, Font font) {
        this.fields.clear();
        this.fieldTypes.clear();

        int maxRows = Math.max(0, (rect.height() - TITLE_HEIGHT) / ROW_HEIGHT);
        int row = 0;
        if (row < maxRows) {
            addVectorRow(rect, row++, font, slot, Component.translatable("gui.yuushya.modeling.pos"), POS_TYPES);
        }
        if (row < maxRows) {
            addVectorRow(rect, row++, font, slot, Component.translatable("gui.yuushya.modeling.rot"), ROT_TYPES);
        }
        if (row < maxRows) {
            addVectorRow(rect, row++, font, slot, Component.translatable("gui.yuushya.modeling.scale"), SCALE_TYPES);
        }
        if (row < maxRows) {
            addBrightnessRow(rect, row, font, slot);
        }
    }

    private int rowY(ModelingLayout.Rect rect, int row) {
        return rect.y() + TITLE_HEIGHT + row * ROW_HEIGHT;
    }

    private void addVectorRow(ModelingLayout.Rect rect, int row, Font font, int slot,
                              Component label, TransformType[] types) {
        int y = rowY(rect, row);
        int startX = rect.x() + LABEL_WIDTH;
        int totalWidth = rect.right() - 6 - startX;
        int fieldWidth = Math.max(20, (totalWidth - GAP * 2) / 3);

        for (int axis = 0; axis < 3; axis++) {
            final int axisIndex = axis;
            TransformType type = types[axisIndex];
            double current = type.extract(this.blockEntity, slot);
            double bound = bounds(type, slot, axisIndex);
            NumberField field = new NumberField(font, startX + axisIndex * (fieldWidth + GAP), y, fieldWidth, ROW_HEIGHT - 2,
                    -bound, bound, scrubStep(type), decimals(type), type == TransformType.SCALE_X,
                    current, value -> apply(type, axisIndex, value));
            this.fields.add(field);
            this.fieldTypes.add(type);
        }
        // 行标签由界面统一绘制（避免每个字段都带一份文本）
        this.rowLabels.put(row, label);
    }

    private void addBrightnessRow(ModelingLayout.Rect rect, int row, Font font, int slot) {
        int y = rowY(rect, row);
        NumberField field = new NumberField(font, rect.x() + LABEL_WIDTH, y, 46, ROW_HEIGHT - 2,
                0.0, 15.0, 1.0, 0, false,
                TransformType.LIT.extract(this.blockEntity, slot),
                value -> this.onChange.accept(TransformType.LIT, value));
        this.fields.add(field);
        this.fieldTypes.add(TransformType.LIT);
        this.rowLabels.put(row, Component.translatable("gui.yuushya.modeling.lit"));
    }

    /** 每行左侧的标签，按行号排列，供界面绘制。 */
    private final Map<Integer, Component> rowLabels = new LinkedHashMap<>();

    public Map<Integer, Component> labels() {
        return this.rowLabels;
    }

    private double bounds(TransformType type, int slot, int axis) {
        var scales = this.blockEntity.getTransformData(slot).scales;
        double scale = switch (axis) {
            case 0 -> scales.x();
            case 1 -> scales.y();
            default -> scales.z();
        };
        return switch (type) {
            case POS_X, POS_Y, POS_Z -> Math.max(16.0, Math.abs(scale) * 16.0 + 16.0);
            case ROT_X, ROT_Y, ROT_Z -> 360.0;
            case SCALE_X, SCALE_Y, SCALE_Z -> 10.0;
            default -> 15.0;
        };
    }

    private static double scrubStep(TransformType type) {
        return switch (type) {
            case POS_X, POS_Y, POS_Z -> 0.5;
            case ROT_X, ROT_Y, ROT_Z -> 1.0;
            case SCALE_X, SCALE_Y, SCALE_Z -> 0.02;
            default -> 1.0;
        };
    }

    private static int decimals(TransformType type) {
        return switch (type) {
            case POS_X, POS_Y, POS_Z, SCALE_X, SCALE_Y, SCALE_Z -> 1;
            default -> 0;
        };
    }

    /** 三个轴的缩放是等比的：拖任意一个都同步三轴。 */
    private void apply(TransformType type, int axis, double value) {
        if (type == TransformType.SCALE_X || type == TransformType.SCALE_Y || type == TransformType.SCALE_Z) {
            double effective = Math.abs(value) <= 0.001 ? 1.0 : value;
            for (TransformType scaleType : SCALE_TYPES) {
                this.onChange.accept(scaleType, effective);
            }
            for (int i = 0; i < this.fieldTypes.size(); i++) {
                if (this.fieldTypes.get(i) == TransformType.SCALE_X || this.fieldTypes.get(i) == TransformType.SCALE_Y
                        || this.fieldTypes.get(i) == TransformType.SCALE_Z) {
                    this.fields.get(i).setValue(effective);
                }
            }
            return;
        }
        this.onChange.accept(type, value);
    }

    public List<AbstractWidget> widgets() {
        return new ArrayList<>(this.fields);
    }

    /** 切换元素后把字段刷到新数据。 */
    public void refresh(int slot) {
        for (int i = 0; i < this.fields.size(); i++) {
            this.fields.get(i).setValue(this.fieldTypes.get(i).extract(this.blockEntity, slot));
        }
    }

    /** 外部改值（例如方块状态变化）后同步字段。 */
    public void sync(TransformType type, double value) {
        for (int i = 0; i < this.fields.size(); i++) {
            if (this.fieldTypes.get(i) == type) {
                this.fields.get(i).setValue(value);
            }
        }
    }

    /** 是否有字段正在输入文本（此时界面不应响应快捷键）。 */
    public boolean isEditing() {
        for (NumberField field : this.fields) {
            if (field.isEditing()) {
                return true;
            }
        }
        return false;
    }

    /** 提交所有正在编辑的字段。 */
    public void commitAll() {
        for (NumberField field : this.fields) {
            field.commitEdit();
        }
    }
}
