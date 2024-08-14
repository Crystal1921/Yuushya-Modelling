package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LazyDoubleRange implements ValidateRange<Double>, StepRange<Double> {
    private final Supplier<Double> minInclusiveSupplier;
    private final Supplier<Double> maxInclusiveSupplier;
    private double step = 0.001;

    public LazyDoubleRange(
            Supplier<Double> minInclusiveSupplier,
            Supplier<Double> maxInclusiveSupplier
    ) {
        this.minInclusiveSupplier = minInclusiveSupplier;
        this.maxInclusiveSupplier = maxInclusiveSupplier;
    }

    @Override
    public Double minInclusive() {
        return minInclusiveSupplier.get();
    }

    @Override
    public Double maxInclusive() {
        return maxInclusiveSupplier.get();
    }

    @Override
    public Optional<Double> validateValue(Double value) {
        if (value < this.minInclusive()) return Optional.of(this.minInclusive());
        if (value > this.maxInclusive()) return Optional.of(this.maxInclusive());
        return Optional.of(Math.floor(value / step) * step);
    }

    @Override
    public void setStep(double step) {this.step = step; }

    @Override
    public double toSliderValue(Double value) {
        return Mth.map(value, minInclusive(), maxInclusive(), 0.0, 1.0);
    }

    @Override
    public Double fromSliderValue(double sliderValue) {
        return Mth.map(sliderValue, 0.0, 1.0, minInclusive(), maxInclusive());
    }

    public static Component captionToString(Component caption, Double value) {
        return Component.empty().append(caption).append(Component.literal(":").append(Component.literal(String.format("%05.1f",value))));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LazyDoubleRange) obj;
        return Objects.equals(this.minInclusiveSupplier.get(), that.minInclusiveSupplier.get()) &&
                Objects.equals(this.maxInclusiveSupplier.get(), that.maxInclusiveSupplier.get()) &&
                Objects.equals(this.step,that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minInclusiveSupplier.get(), maxInclusiveSupplier.get(),step);
    }

    @Override
    public String toString() {
        return "LazyDoubleRange[" +
                "minInclusive=" + minInclusiveSupplier.get() + ", " +
                "maxInclusive=" + maxInclusiveSupplier.get() + ", " +
                "step=" + step + ']';
    }

    public static ButtonBuilder buttonBuilder(Component caption,Supplier<Double> minInclusiveSupplier, Supplier<Double> maxInclusiveSupplier, Consumer<Double> onValueChanged){
        return new ButtonBuilder(caption,minInclusiveSupplier,maxInclusiveSupplier,onValueChanged);
    }

    public static class ButtonBuilder extends SliderButton.Builder<Double> {
        public ButtonBuilder(Component caption, Supplier<Double> minInclusiveSupplier, Supplier<Double> maxInclusiveSupplier, Consumer<Double> onValueChanged) {
            super(caption, new LazyDoubleRange(minInclusiveSupplier, maxInclusiveSupplier), onValueChanged);
            this.text(LazyDoubleRange::captionToString);
            this.initial(minInclusiveSupplier.get());
        }
    }
}
