package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.Component;
import com.yuushya.modelling.utils.YuushyaUtils.Mth;

import java.util.function.Consumer;

public class DividedDoubleRange extends DoubleRange{
    private final Double middle;
    public DividedDoubleRange(Double minInclusive, Double middle, Double maxInclusive) {
        super(minInclusive, maxInclusive);
        this.middle= middle;
    }

    @Override
    public Double fromSliderValue(double sliderValue) {
        if(sliderValue<=0.5)
            return Mth.map(sliderValue, 0.0, 0.5, minInclusive, middle);
        else
            return Mth.map(sliderValue, 0.5, 1.0, middle, maxInclusive);
    }

    @Override
    public double toSliderValue(Double value) {
        if(value<=middle)
            return Mth.map(value, minInclusive, middle, 0.0f, 0.5f);
        else
            return Mth.map(value, middle, maxInclusive, 0.5f, 1.0f);
    }

    public static ButtonBuilder buttonBuilder(Component caption, Double minInclusive, Double middle, Double maxInclusive, Consumer<Double> onValueChanged){
        return new ButtonBuilder(caption,minInclusive,middle,maxInclusive,onValueChanged);
    }

    public static class ButtonBuilder extends SliderButton.Builder<Double> {
        public ButtonBuilder(Component caption, Double minInclusive, Double middle, Double maxInclusive, Consumer<Double> onValueChanged) {
            super(caption, new DividedDoubleRange(minInclusive, middle, maxInclusive), onValueChanged);
            this.text(LazyDoubleRange::captionToString);
            this.initial(minInclusive);
        }
    }
}
