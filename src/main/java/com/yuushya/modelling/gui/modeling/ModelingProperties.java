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
 * 直接拖动即可改值。数值没有上下限，与建模软件一致 —— 不会拖到某个值就停住。
 */
public final class ModelingProperties {

    private static final int ROW_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 12;
    private static final int LABEL_WIDTH = 46;
    private static final int GAP = 3;

    private static final TransformType[] POS_TYPES = {TransformType.POS_X, TransformType.POS_Y, TransformType.POS_Z};
    private static final TransformType[] ROT_TYPES = {TransformType.ROT_X, TransformType.ROT_Y, TransformType.ROT_Z};
    private static final TransformType[] SCALE_TYPES = {TransformType.SCALE_X, TransformType.SCALE_Y, TransformType.SCALE_Z};

    /**
     * 光源等级的取值范围，必须与 {@code YuushyaBlockStates.LIT} 的定义一致
     * （{@code IntegerProperty.create("lit", 0, 15)}）。越界会让 {@code setValue} 抛异常。
     */
    public static final double LIT_MIN = 0.0;
    public static final double LIT_MAX = 15.0;

    private final ShowBlockEntity blockEntity;
    private final BiConsumer<TransformType, Double> onChange;
    private final List<NumberField> fields = new ArrayList<>();
    private final List<TransformType> fieldTypes = new ArrayList<>();
    private final Map<Integer, Component> rowLabels = new LinkedHashMap<>();
    private int slot;

    public ModelingProperties(ShowBlockEntity blockEntity, BiConsumer<TransformType, Double> onChange) {
        this.blockEntity = blockEntity;
        this.onChange = onChange;
    }

    /** 在给定区域内排布四组：位移 / 旋转 / 缩放 / 亮度。 */
    public void layout(ModelingLayout.Rect rect, int slot, Font font) {
        this.fields.clear();
        this.fieldTypes.clear();
        this.rowLabels.clear();
        this.slot = slot;

        int maxRows = Math.max(0, (rect.height() - TITLE_HEIGHT) / ROW_HEIGHT);
        int row = 0;
        if (row < maxRows) {
            addVectorRow(rect, row++, font, Component.translatable("gui.yuushya.modeling.pos"), POS_TYPES);
        }
        if (row < maxRows) {
            addVectorRow(rect, row++, font, Component.translatable("gui.yuushya.modeling.rot"), ROT_TYPES);
        }
        if (row < maxRows) {
            addVectorRow(rect, row++, font, Component.translatable("gui.yuushya.modeling.scale"), SCALE_TYPES);
        }
        if (row < maxRows) {
            addBrightnessRow(rect, row, font);
        }
    }

    private int rowY(ModelingLayout.Rect rect, int row) {
        return rect.y() + TITLE_HEIGHT + row * ROW_HEIGHT;
    }

    private void addVectorRow(ModelingLayout.Rect rect, int row, Font font,
                              Component label, TransformType[] types) {
        int y = rowY(rect, row);
        int startX = rect.x() + LABEL_WIDTH;
        int totalWidth = rect.right() - 6 - startX;
        int fieldWidth = Math.max(20, (totalWidth - GAP * 2) / 3);

        for (int axis = 0; axis < 3; axis++) {
            final int axisIndex = axis;
            TransformType type = types[axis];
            NumberField field = new NumberField(font,
                    startX + axisIndex * (fieldWidth + GAP), y, fieldWidth, ROW_HEIGHT - 2,
                    scrubStep(type), decimals(type),
                    type.extract(this.blockEntity, this.slot),
                    value -> apply(type, axisIndex, value));
            this.fields.add(field);
            this.fieldTypes.add(type);
        }
        this.rowLabels.put(row, label);
    }

    private void addBrightnessRow(ModelingLayout.Rect rect, int row, Font font) {
        int y = rowY(rect, row);
        NumberField field = new NumberField(font, rect.x() + LABEL_WIDTH, y, 46, ROW_HEIGHT - 2,
                1.0, 0, LIT_MIN, LIT_MAX,
                TransformType.LIT.extract(this.blockEntity, this.slot),
                value -> this.onChange.accept(TransformType.LIT, value));
        this.fields.add(field);
        this.fieldTypes.add(TransformType.LIT);
        this.rowLabels.put(row, Component.translatable("gui.yuushya.modeling.lit"));
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
        if (isScale(type)) {
            double effective = Math.abs(value) <= 0.001 ? 1.0 : value;
            for (TransformType scaleType : SCALE_TYPES) {
                this.onChange.accept(scaleType, effective);
            }
            for (int i = 0; i < this.fieldTypes.size(); i++) {
                if (isScale(this.fieldTypes.get(i))) {
                    this.fields.get(i).setValue(effective);
                }
            }
            return;
        }
        this.onChange.accept(type, value);
    }

    private static boolean isScale(TransformType type) {
        return type == TransformType.SCALE_X || type == TransformType.SCALE_Y || type == TransformType.SCALE_Z;
    }

    public List<AbstractWidget> widgets() {
        return new ArrayList<>(this.fields);
    }

    /** 切换元素后把字段刷到新数据。 */
    public void refresh(int slot) {
        this.slot = slot;
        for (int i = 0; i < this.fields.size(); i++) {
            this.fields.get(i).setValue(this.fieldTypes.get(i).extract(this.blockEntity, slot));
        }
    }

    /** 外部改值后同步字段显示。 */
    public void sync(TransformType type, double value) {
        for (int i = 0; i < this.fields.size(); i++) {
            if (this.fieldTypes.get(i) == type) {
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
