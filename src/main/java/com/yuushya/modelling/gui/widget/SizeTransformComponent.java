package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class SizeTransformComponent {
    public ItemTransformType type;
    public double standardStep;
    public double fine_tuneStep = 0.001;
    public SliderButton<Double> sliderButton;
    public Button minusButton;
    public Button addButton;
    public EditBox editBox;
    public Button cancelButton;
    public Button finishButton;
    private final int PER_HEIGHT = 20;

    public SizeTransformComponent(ItemTransformType type) {
        this.type = type;
    }

    public double setStandardStep(double step) {
        this.standardStep = step;
        return step;
    }

    public void setSliderInitial(ItemBlockEntity blockEntity, int slot) {
        double step = sliderButton.getStep();
        sliderButton.setStep(fine_tuneStep);
        sliderButton.setInitialValidatedValue(type.extract(blockEntity, slot));
        sliderButton.setStep(step);
        if (editBox != null) {
            editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
        }
    }

    public void setSliderStep() {
        sliderButton.setStep(standardStep);
    }

    public void setSliderFineTune() {
        sliderButton.setStep(fine_tuneStep);
    }

    public void initWidget(Font font) {
        // Position buttons relative to slider button (like the original TransformComponent)
        minusButton = Button.builder(Component.literal("-"), (btn) -> {
            double currentValue = sliderButton.getValidatedValue();
            double newValue = currentValue - sliderButton.getStep();
            sliderButton.setValidatedValue(newValue);
        }).bounds(sliderButton.getX() - 10, sliderButton.getY(), 10, PER_HEIGHT).build();

        addButton = Button.builder(Component.literal("+"), (btn) -> {
            double currentValue = sliderButton.getValidatedValue();
            double newValue = currentValue + sliderButton.getStep();
            sliderButton.setValidatedValue(newValue);
        }).bounds(sliderButton.getX() + sliderButton.getWidth(), sliderButton.getY(), 10, PER_HEIGHT).build();

        // Initialize edit box with proper positioning
        editBox = new EditBox(font, sliderButton.getX(), sliderButton.getY() + PER_HEIGHT, sliderButton.getWidth(), PER_HEIGHT, Component.empty());
        editBox.setMaxLength(10);
        editBox.visible = false;

        cancelButton = Button.builder(Component.literal("×").withStyle(ChatFormatting.RED), (btn) -> {
            editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
            triggerVisible(true);
        }).bounds(sliderButton.getX() - 10, sliderButton.getY() + PER_HEIGHT, 10, PER_HEIGHT).build();
        cancelButton.visible = false;

        finishButton = Button.builder(Component.literal("✓").withStyle(ChatFormatting.GREEN), (btn) -> {
            saveEditBoxValue();
            triggerVisible(true);
        }).bounds(sliderButton.getX() + sliderButton.getWidth(), sliderButton.getY() + PER_HEIGHT, 10, PER_HEIGHT).build();
        finishButton.visible = false;
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
        // 更新滑条位置并直接触发回调，保证点√后立即生效（不依赖滑条位置是否变化）
        sliderButton.setInitialValidatedValue(number);
        sliderButton.applyValidatedValue(number);
        setEditBoxInitial();
    }

    public void setEditBoxInitial() {
        editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
    }

    public void triggerVisible(boolean sliderVisible) {
        sliderButton.visible = sliderVisible;
        minusButton.visible = sliderVisible;
        addButton.visible = sliderVisible;
        editBox.visible = sliderVisible;
        cancelButton.visible = sliderVisible;
        finishButton.visible = sliderVisible;

        editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
    }
}