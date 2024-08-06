package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;

public enum UnitDoubleRange implements ValidateRange<Double> {
    INSTANCE;
    @Override
    public Optional<Double> validateValue(Double value) {
        return value >= 0.0 && value <= 1.0 ? Optional.of(value) : Optional.empty();
    }

    @Override
    public double toSliderValue(Double value) {
        return value;
    }

    @Override
    public Double fromSliderValue(double sliderValue) {
        return sliderValue;
    }


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
    public static AbstractWidget createButton(Component caption, int x, int y, int width, int height, Consumer<Double> onValueChanged){
        return new SliderButton<Double>(caption,x,y,width,height, OptionInstance.noTooltip(), UnitDoubleRange::captionToString,INSTANCE,1.0,onValueChanged);
    }
}
