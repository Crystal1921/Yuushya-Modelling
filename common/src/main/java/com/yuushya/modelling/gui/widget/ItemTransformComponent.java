package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.SliderButton;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ItemTransformComponent {
    public ItemTransformType type;
    public double standardStep;
    public double fine_tuneStep = 0.001;
    public SliderButton<Double> sliderButton;
    public Button minusButton;
    public Button addButton;
    public EditBox editBox;
    public Button cancelButton;
    public Button finishButton;

    public ItemTransformComponent(ItemTransformType type) {
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
    }

    public void setSliderStep() {
        sliderButton.setStep(standardStep);
    }

    public void setSliderFineTune() {
        sliderButton.setStep(fine_tuneStep);
    }

    public void initWidget(Font font) {
        // Initialize edit-related widgets similar to TransformComponent
        editBox = new EditBox(font, 0, 0, 80, 20, Component.empty());
        editBox.visible = false;
        
        cancelButton = Button.builder(Component.literal("✖").withStyle(ChatFormatting.RED), (btn) -> {
            triggerVisible(true);
        }).size(10, 10).build();
        cancelButton.visible = false;
        
        finishButton = Button.builder(Component.literal("✓").withStyle(ChatFormatting.GREEN), (btn) -> {
            try {
                double value = Double.parseDouble(editBox.getValue());
                if (sliderButton instanceof ValidateRange<Double> validateRange) {
                    if (validateRange.isValidValue(value)) {
                        sliderButton.setInitialValidatedValue(value);
                        triggerVisible(true);
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid number, ignore
            }
        }).size(10, 10).build();
        finishButton.visible = false;
        
        // Initialize increment/decrement buttons
        minusButton = Button.builder(Component.literal("-"), (btn) -> {
            double currentValue = sliderButton.getValidatedValue();
            double newValue = currentValue - sliderButton.getStep();
            sliderButton.setValidatedValue(newValue);
        }).size(10, 10).build();
        
        addButton = Button.builder(Component.literal("+"), (btn) -> {
            double currentValue = sliderButton.getValidatedValue();
            double newValue = currentValue + sliderButton.getStep();
            sliderButton.setValidatedValue(newValue);
        }).size(10, 10).build();
    }

    public void triggerVisible(boolean sliderVisible) {
        sliderButton.visible = sliderVisible;
        minusButton.visible = sliderVisible;
        addButton.visible = sliderVisible;
        editBox.visible = !sliderVisible;
        cancelButton.visible = !sliderVisible;
        finishButton.visible = !sliderVisible;
        
        if (!sliderVisible) {
            editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));
        }
    }
}