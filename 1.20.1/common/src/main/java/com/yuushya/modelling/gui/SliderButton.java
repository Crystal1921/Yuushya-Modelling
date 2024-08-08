package com.yuushya.modelling.gui;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Consumer;

public class SliderButton<T extends Comparable<T>> extends AbstractSliderButton {
    //private static final Logger LOGGER = LogUtils.getLogger();
    private final OptionInstance.CaptionBasedToString<T> captionBasedToString;
    private final T initialValue;
    private final Component caption;
    private final ValidateRange<T> validateRange;
    private final OptionInstance.TooltipSupplier<T> tooltipSupplier;
    private final Consumer<T> onValueChanged;

    public SliderButton(Component caption,
                        int x, int y, int width, int height,
                        OptionInstance.TooltipSupplier<T> tooltipSupplier,
                        OptionInstance.CaptionBasedToString<T> captionBasedToString,
                        ValidateRange<T> validateRange,
                        T initialValue,
                        Consumer<T> onValueChanged) {
        super(x, y, width, height, CommonComponents.EMPTY, validateRange.toSliderValue(initialValue));
        this.caption = caption;
        this.initialValue = initialValue;
        this.validateRange = validateRange;
        this.tooltipSupplier = tooltipSupplier;
        this.captionBasedToString = captionBasedToString;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        T value = this.validateRange.fromSliderValue(this.value);
        this.setMessage(captionBasedToString.toString(this.caption,value));
        this.setTooltip(this.tooltipSupplier.apply(value));
    }

    @Override
    protected void applyValue() {
        T value = this.validateRange.fromSliderValue(this.value);
        T object = this.validateRange.validateValue(value).orElseGet(() -> {
            //LOGGER.error("Illegal option value " + value + " for " + this.caption);
            return this.initialValue;
        });
        this.onValueChanged.accept(object);
    }

    public static <R extends Comparable<R>> Builder<R> builder(Component caption, ValidateRange<R> validateRange, Consumer<R> onValueChanged){
        return new Builder<>(caption,validateRange,onValueChanged);
    }

    public void setValue(double value) {
        double d = this.value;
        this.value = Mth.clamp(value, 0.0, 1.0);
        if (d != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    public void setStep(double step){
        this.validateRange.setStep(step);
    }

    public static class Builder<R extends Comparable<R>> {
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private OptionInstance.CaptionBasedToString<R> captionBasedToString = (caption,number) -> caption;
        private OptionInstance.TooltipSupplier<R> tooltipSupplier = OptionInstance.noTooltip();
        private R initialValue;
        private final Component caption;
        private final ValidateRange<R> validateRange;
        private final Consumer<R> onValueChanged;
        private double step = 0.001;

        public Builder(Component caption, ValidateRange<R> validateRange, Consumer<R> onValueChanged) {
            this.caption = caption;
            this.onValueChanged = onValueChanged;
            this.validateRange = validateRange;
            this.initialValue = validateRange.defaultValue();
        }

        public Builder<R> pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder<R> width(int width) {
            this.width = width;
            return this;
        }

        public Builder<R> size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder<R> bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public Builder<R> text(OptionInstance.CaptionBasedToString<R> textCalculate){
            this.captionBasedToString = textCalculate;
            return this;
        }

        public Builder<R> tooltip(OptionInstance.TooltipSupplier<R> tooltipSupplier){
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public Builder<R> initial(R number){
            this.initialValue = number;
            return this;
        }

        public Builder<R> step(double step){
            this.step = step;
            return this;
        }

        public SliderButton<R> build(){
            validateRange.setStep(step);
            return new SliderButton<R>(caption,x,y,width,height,tooltipSupplier,captionBasedToString,validateRange,initialValue,onValueChanged);
        }
    }

}
