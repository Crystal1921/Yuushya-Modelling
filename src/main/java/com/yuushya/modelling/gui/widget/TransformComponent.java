package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.gui.AbstractTransformScreen;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 一行变换滑块的通用实现：滑块 + 加减按钮 + 可输入的编辑框。
 * <p>
 * 之前 Show/Item/Text 三个界面各自维护了一份几乎相同的组件类，这里通过
 * {@link Style} 保留它们在编辑框位置、按钮行为上的差异，做到行为不变。
 */
public final class TransformComponent<T> {

    /** 滑块行的布局与交互风格。 */
    public enum Style {
        /** 滑块与编辑框重叠放置，编辑框内的取消/确认按钮不退出编辑模式（Show/Text 使用）。 */
        SLIDER_ROW,
        /** 滑块与编辑框重叠放置，编辑框内的取消/确认按钮会退回滑块模式（Item 使用）。 */
        ITEM_ROW,
        /** 编辑框位于滑块下方，且始终与滑块一起显示（Item 的自定义缩放使用）。 */
        SIZE_ROW
    }

    public final T type;
    public final Style style;
    public double standardStep;
    public double fine_tuneStep = 0.001;
    public SliderButton<Double> sliderButton;
    public Button minusButton;
    public Button addButton;
    public Button cancelButton;
    public Button finishButton;
    public EditBox editBox;

    /** 编辑框内显示的轴/分量装饰文本。 */
    private Component decoration = Component.empty();
    private int editBoxMaxLength = 15;

    public TransformComponent(T type) {
        this(type, Style.SLIDER_ROW);
    }

    public TransformComponent(T type, Style style) {
        this.type = type;
        this.style = style;
        this.editBoxMaxLength = style == Style.SIZE_ROW ? 10 : 15;
    }

    public TransformComponent<T> decoration(Component decoration) {
        this.decoration = decoration;
        return this;
    }

    public double setStandardStep(double step) {
        this.standardStep = step;
        return step;
    }

    public void setSliderInitial(double value) {
        double step = sliderButton.getStep();
        sliderButton.setStep(fine_tuneStep);
        sliderButton.setInitialValidatedValue(value);
        sliderButton.setStep(step);
        if (this.style != Style.SLIDER_ROW && editBox != null) {
            editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
        }
    }

    public void setSliderStep() {
        sliderButton.setStep(standardStep);
    }

    public void setSliderFineTune() {
        sliderButton.setStep(fine_tuneStep);
    }

    public void step(boolean increase) {
        double delta = sliderButton.getStep();
        sliderButton.setValidatedValue(sliderButton.getValidatedValue() + (increase ? delta : -delta));
    }

    public void setEditBoxInitial() {
        editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
    }

    public void saveEditBoxValue() {
        double number;
        try {
            number = Double.parseDouble(editBox.getValue());
        } catch (NumberFormatException ignored) {
            number = sliderButton.getValidatedValue();
        }
        ValidateRange<Double> validateRange = sliderButton.getValidateRange();
        if (validateRange instanceof LazyDoubleRange doubleValidateRange) {
            double min = doubleValidateRange.minInclusive();
            double max = doubleValidateRange.maxInclusive();
            if (number < min) {
                double finalNumber = number;
                doubleValidateRange.setMinInclusiveSupplier(() -> finalNumber);
            }

            if (number > max) {
                double finalNumber = number;
                doubleValidateRange.setMaxInclusiveSupplier(() -> finalNumber);
            }
        }
        sliderButton.setValidatedValue(number);
        setEditBoxInitial();
    }

    /** 在滑块模式与编辑框模式之间切换。 */
    public void triggerVisible(boolean sliderVisible) {
        sliderButton.visible = sliderVisible;
        minusButton.visible = sliderVisible;
        addButton.visible = sliderVisible;
        if (style == Style.SIZE_ROW) {
            editBox.setVisible(sliderVisible);
            cancelButton.visible = sliderVisible;
            finishButton.visible = sliderVisible;
            editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
            return;
        }
        if (style == Style.SLIDER_ROW && !sliderVisible) {
            setEditBoxInitial();
        }
        editBox.setVisible(!sliderVisible);
        cancelButton.visible = !sliderVisible;
        finishButton.visible = !sliderVisible;
    }

    public void setInvisible() {
        sliderButton.visible = false;
        minusButton.visible = false;
        addButton.visible = false;
        editBox.setVisible(false);
        cancelButton.visible = false;
        finishButton.visible = false;
    }

    private Component editBoxComponent() {
        if (style != Style.SLIDER_ROW) return Component.empty();
        return Component.empty().append(sliderButton.getCaption()).append(decoration);
    }

    public void initWidget(Font font) {
        int editBoxY = style == Style.SIZE_ROW ? sliderButton.getY() + AbstractTransformScreen.PER_HEIGHT : sliderButton.getY();

        minusButton = Button.builder(Component.literal("-"), (btn) -> step(false))
                .bounds(sliderButton.getX() - AbstractTransformScreen.SMALL_BUTTON_WIDTH, sliderButton.getY(), AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();
        addButton = Button.builder(Component.literal("+"), (btn) -> step(true))
                .bounds(sliderButton.getX() + sliderButton.getWidth(), sliderButton.getY(), AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();

        editBox = new EditBox(font, sliderButton.getX(), editBoxY, sliderButton.getWidth(), AbstractTransformScreen.PER_HEIGHT, editBoxComponent());
        editBox.setMaxLength(editBoxMaxLength);

        if (style == Style.SLIDER_ROW) {
            cancelButton = Button.builder(Component.literal("×"), (btn) -> setEditBoxInitial())
                    .bounds(sliderButton.getX() - AbstractTransformScreen.SMALL_BUTTON_WIDTH, editBoxY, AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();
            finishButton = Button.builder(Component.literal("√"), (btn) -> saveEditBoxValue())
                    .bounds(sliderButton.getX() + sliderButton.getWidth(), editBoxY, AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();
            triggerVisible(true);
        } else {
            cancelButton = Button.builder(Component.literal("×").withStyle(ChatFormatting.RED), (btn) -> {
                editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
                triggerVisible(true);
            }).bounds(sliderButton.getX() - AbstractTransformScreen.SMALL_BUTTON_WIDTH, editBoxY, AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();
            finishButton = Button.builder(Component.literal("✓").withStyle(ChatFormatting.GREEN), (btn) -> {
                saveEditBoxValue();
                triggerVisible(true);
            }).bounds(sliderButton.getX() + sliderButton.getWidth(), editBoxY, AbstractTransformScreen.SMALL_BUTTON_WIDTH, AbstractTransformScreen.PER_HEIGHT).build();
            editBox.visible = false;
            cancelButton.visible = false;
            finishButton.visible = false;
        }
        setEditBoxInitial();
    }
}
