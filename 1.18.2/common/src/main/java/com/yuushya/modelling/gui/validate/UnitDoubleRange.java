package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.function.Consumer;

public record UnitDoubleRange() implements ValidateRange<Double> {

    @Override
    public double toSliderValue(Double value) {
        return value;
    }

    @Override
    public Double fromSliderValue(double sliderValue) { return sliderValue; }

    @Override
    public Double minInclusive() { return 0.0; }

    @Override
    public Double maxInclusive() { return 1.0; }

    //do nothing
    @Override
    public void setStep(Double step) { }

    @Override
    public Double getStep() { return 0.001; }

    public static Component percentValueLabel(Component text, double value) {
        return new TranslatableComponent("options.percent_value", text, (int)(value * 100.0));
    }
    public static Component genericValueLabel(Component text, Component value) {
        return new TranslatableComponent("options.generic_value", text, value);
    }
    public static Component captionToString(Component caption, Double value){
        if (value == 0.0) {
            return genericValueLabel(caption, CommonComponents.OPTION_OFF);
        }
        return percentValueLabel(caption, value);
    }

    public static ButtonBuilder buttonBuilder(Component caption, Consumer<Double> onValueChanged){
        return new ButtonBuilder(caption,onValueChanged);
    }

    public static class ButtonBuilder extends SliderButton.Builder<Double> {
        public ButtonBuilder(Component caption, Consumer<Double> onValueChanged) {
            super(caption, new UnitDoubleRange(), onValueChanged);
            this.text(UnitDoubleRange::captionToString);
            this.initial(0.0);
        }
    }
}
