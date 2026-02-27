package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TransformComponent {
    public TransformType type;
    public double standardStep;
    public double fine_tuneStep = 0.001;
    public SliderButton<Double> sliderButton;
    public Button minusButton;
    public Button addButton;
    public EditBox editBox;
    public Button cancelButton;
    public Button finishButton;

    public TransformComponent(TransformType type) {
        this.type = type;
    }

    public double setStandardStep(double step) {
        this.standardStep = step;
        return step;
    }

    public void setSliderInitial(ShowBlockEntity blockEntity, int slot) {
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

    public void step(boolean increase) {
        if (increase) sliderButton.setValidatedValue(sliderButton.getValidatedValue() + sliderButton.getStep());
        else sliderButton.setValidatedValue(sliderButton.getValidatedValue() - sliderButton.getStep());
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

    public void triggerVisible(boolean sliderVisible) {
        sliderButton.visible = sliderVisible;
        addButton.visible = sliderVisible;
        minusButton.visible = sliderVisible;
        if (!sliderVisible) {
            setEditBoxInitial();
        }
        editBox.setVisible(!sliderVisible);
        finishButton.visible = !sliderVisible;
        cancelButton.visible = !sliderVisible;
    }

    Component editBoxComponent() {
        MutableComponent component = switch (type) {
            case POS_X, ROT_X ->
                    Component.translatable("block.yuushya.showblock.x", "").withStyle(ChatFormatting.DARK_RED);
            case POS_Y, ROT_Y ->
                    Component.translatable("block.yuushya.showblock.y", "").withStyle(ChatFormatting.GREEN);
            case POS_Z, ROT_Z -> Component.translatable("block.yuushya.showblock.z", "").withStyle(ChatFormatting.BLUE);
            case SCALE_X -> Component.translatable("gui.yuushya.showBlockScreen.scale_text", "");
            default -> Component.empty();
        };
        return Component.empty().append(sliderButton.getCaption()).append(component);
    }

    public void initWidget(Font font) {
        minusButton = Button.builder(Component.literal("-"), (btn) -> step(false))
                .bounds(sliderButton.getX() - ShowBlockScreen.SMALL_BUTTON_WIDTH, sliderButton.getY(), ShowBlockScreen.SMALL_BUTTON_WIDTH, ShowBlockScreen.PER_HEIGHT).build();
        addButton = Button.builder(Component.literal("+"), (btn) -> step(true))
                .bounds(sliderButton.getX() + sliderButton.getWidth(), sliderButton.getY(), ShowBlockScreen.SMALL_BUTTON_WIDTH, ShowBlockScreen.PER_HEIGHT).build();
        editBox = new EditBox(font, sliderButton.getX(), sliderButton.getY(), sliderButton.getWidth(), ShowBlockScreen.PER_HEIGHT, editBoxComponent());
        editBox.setMaxLength(15);
        setEditBoxInitial();

        cancelButton = Button.builder(Component.literal("×"), (btn) -> setEditBoxInitial())
                .bounds(sliderButton.getX() - ShowBlockScreen.SMALL_BUTTON_WIDTH, sliderButton.getY(), ShowBlockScreen.SMALL_BUTTON_WIDTH, ShowBlockScreen.PER_HEIGHT).build();
        finishButton = Button.builder(Component.literal("√"), (btn) -> saveEditBoxValue())
                .bounds(sliderButton.getX() + sliderButton.getWidth(), sliderButton.getY(), ShowBlockScreen.SMALL_BUTTON_WIDTH, ShowBlockScreen.PER_HEIGHT).build();
        triggerVisible(true);
    }
}
