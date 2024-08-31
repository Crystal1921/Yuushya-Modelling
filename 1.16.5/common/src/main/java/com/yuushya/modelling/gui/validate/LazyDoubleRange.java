package com.yuushya.modelling.gui.validate;

import com.yuushya.modelling.gui.SliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import com.yuushya.modelling.utils.YuushyaUtils.Mth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.yuushya.modelling.gui.showblock.ShowBlockScreen.componentEmpty;

public final class LazyDoubleRange implements ValidateRange<Double>, StepRange<Double> {
    private final Supplier<Double> minInclusiveSupplier;
    private final Supplier<Double> maxInclusiveSupplier;
    private double step = 0.001;
    private Supplier<Double> stepSupplier;

    public LazyDoubleRange(
            Supplier<Double> minInclusiveSupplier,
            Supplier<Double> maxInclusiveSupplier,
            Supplier<Double> stepSupplier
    ){
        this(minInclusiveSupplier,maxInclusiveSupplier);
        this.stepSupplier = stepSupplier;
        this.step = 0;
    }

    public LazyDoubleRange(
            Supplier<Double> minInclusiveSupplier,
            Supplier<Double> maxInclusiveSupplier
    ) {
        this.minInclusiveSupplier = minInclusiveSupplier;
        this.maxInclusiveSupplier = maxInclusiveSupplier;
        this.stepSupplier =  ()->step;
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
        return Optional.of(Math.floor(value / getStep()) * getStep());
    }

    public void setStepSupplier(Supplier<Double> stepSupplier) { this.stepSupplier = stepSupplier;}

    @Override
    public void setStep(Double step) {this.step = step; }

    @Override
    public Double getStep() {
        return (step == 0.0) ? stepSupplier.get() : step;
    }

    @Override
    public double toSliderValue(Double value) {
        return Mth.map(value, minInclusive(), maxInclusive(), 0.0, 1.0);
    }

    @Override
    public Double fromSliderValue(double sliderValue) {
        return Mth.map(sliderValue, 0.0, 1.0, minInclusive(), maxInclusive());
    }

    public static Component captionToString(Component caption, Double value) {
        return componentEmpty().append(caption).append(new TextComponent(":").append(new TextComponent(String.format("%05.1f",value))));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        LazyDoubleRange that = (LazyDoubleRange) obj;
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

    public static ButtonBuilder buttonBuilder(Component caption,Supplier<Double> minInclusiveSupplier, Supplier<Double> maxInclusiveSupplier, Supplier<Double> stepSupplier,  Consumer<Double> onValueChanged){
        return new ButtonBuilder(caption,minInclusiveSupplier,maxInclusiveSupplier,stepSupplier,onValueChanged);
    }

    public static class ButtonBuilder extends SliderButton.Builder<Double> {
        public ButtonBuilder(Component caption, Supplier<Double> minInclusiveSupplier, Supplier<Double> maxInclusiveSupplier, Consumer<Double> onValueChanged) {
            super(caption, new LazyDoubleRange(minInclusiveSupplier, maxInclusiveSupplier), onValueChanged);
            this.text(LazyDoubleRange::captionToString);
            this.initial(minInclusiveSupplier.get());
        }
        public ButtonBuilder(Component caption, Supplier<Double> minInclusiveSupplier, Supplier<Double> maxInclusiveSupplier, Supplier<Double> stepSupplier, Consumer<Double> onValueChanged) {
            super(caption, new LazyDoubleRange(minInclusiveSupplier, maxInclusiveSupplier, stepSupplier), onValueChanged);
            this.text(LazyDoubleRange::captionToString);
            this.initial(minInclusiveSupplier.get());
        }


    }
}
