package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class UnitDoubleRange implements ValidateRange<Double> {

    private double step = 0.001;

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

    @Override
    public void setStep(double step) {this.step = step; }

    public static Component percentValueLabel(Component text, double value) {
        return Component.translatable("options.percent_value", text, (int)(value * 100.0));
    }
    public static Component genericValueLabel(Component text, Component value) {
        return Component.translatable("options.generic_value", text, value);
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
