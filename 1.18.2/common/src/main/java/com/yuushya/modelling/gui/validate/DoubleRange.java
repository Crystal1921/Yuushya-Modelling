package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class DoubleRange implements ValidateRange<Double>, StepRange<Double> {
    protected final Double minInclusive;
    protected final Double maxInclusive;
    private double step = 0.001;

    public DoubleRange(Double minInclusive, Double maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    @Override
    public Optional<Double> validateValue(Double value) {
        if (value < this.minInclusive()) return Optional.of(this.minInclusive());
        if (value > this.maxInclusive()) return Optional.of(this.maxInclusive());
        return Optional.of(Math.floor(value / step) * step);
    }

    @Override
    public double toSliderValue(Double value) {
        return Mth.map(value, minInclusive, maxInclusive, 0.0f, 1.0f);
    }

    @Override
    public Double fromSliderValue(double sliderValue) {
        return Mth.map(sliderValue, 0.0, 1.0, (double) minInclusive, (double) maxInclusive);
    }

    @Override
    public Double minInclusive() { return minInclusive; }

    @Override
    public Double maxInclusive() { return maxInclusive; }

    @Override
    public void setStep(Double step) {this.step = step; }

    @Override
    public Double getStep() { return step; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DoubleRange) obj;
        return Objects.equals(this.minInclusive, that.minInclusive) &&
                Objects.equals(this.maxInclusive, that.maxInclusive) &&
                Objects.equals(this.step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minInclusive, maxInclusive, step);
    }

    @Override
    public String toString() {
        return "DoubleRange[" +
                "minInclusive=" + minInclusive + ", " +
                "maxInclusive=" + maxInclusive + ", " +
                "step=" + step + ']';
    }

    public static ButtonBuilder buttonBuilder(Component caption,  Double minInclusive, Double maxInclusive, Consumer<Double> onValueChanged){
        return new ButtonBuilder(caption,minInclusive,maxInclusive,onValueChanged);
    }

    public static class ButtonBuilder extends SliderButton.Builder<Double> {
        public ButtonBuilder(Component caption, Double minInclusive, Double maxInclusive, Consumer<Double> onValueChanged) {
            super(caption, new DoubleRange(minInclusive, maxInclusive), onValueChanged);
            this.text(LazyDoubleRange::captionToString);
            this.initial(minInclusive);
        }
    }


}
