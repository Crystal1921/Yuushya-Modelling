package com.yuushya.modelling.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class SliderButton<T extends Comparable<T>> extends AbstractSliderButton implements TooltipAccessor {
    //private static final Logger LOGGER = LogUtils.getLogger();
    private final CaptionBasedToString<T> captionBasedToString;
    private final T initialValue;
    private final Component caption;
    public Component getCaption() { return caption; }
    private final ValidateRange<T> validateRange;
    private final CycleButton.TooltipSupplier<T> tooltipSupplier;
    private final Consumer<SliderButton<T>> onMouseOver;
    private final Consumer<T> onValueChanged;
    public SliderButton(Component caption,
                        int x, int y, int width, int height,
                        CycleButton.TooltipSupplier<T> tooltipSupplier,
                        CaptionBasedToString<T> captionBasedToString,
                        ValidateRange<T> validateRange,
                        T initialValue,
                        Consumer<SliderButton<T>> onMouseOver,
                        Consumer<T> onValueChanged) {
        super(x, y, width, height, TextComponent.EMPTY , validateRange.toSliderValue(initialValue));
        this.caption = caption;
        this.initialValue = initialValue;
        this.validateRange = validateRange;
        this.tooltipSupplier = tooltipSupplier;
        this.captionBasedToString = captionBasedToString;
        this.onMouseOver = onMouseOver;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderButton(poseStack, mouseX, mouseY, partialTick);
        if(this.isHovered){
            this.onMouseOver.accept(this);
        }
    }

    @Override
    protected void updateMessage() {
        T value = this.validateRange.fromSliderValue(this.value);
        T object = this.validateRange.validateValue(value).orElseGet(() -> this.initialValue);
        this.setMessage(captionBasedToString.toString(this.caption,object));
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

    public void setValidatedValue(T value){
        this.setValue(this.validateRange.toSliderValue(value));
    }

    public T getValidatedValue(){
        return this.validateRange.fromSliderValue(this.value);
    }

    public void setValue(double value) {
        double d = this.value;
        this.value = Mth.clamp(value, 0.0, 1.0);
        if (d != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    public void setStep(T step){ this.validateRange.setStep(step); }
    public T getStep(){ return this.validateRange.getStep(); }

    @Override
    public List<FormattedCharSequence> getTooltip() {
        return this.tooltipSupplier.apply(this.validateRange.fromSliderValue(this.value));
    }

    public static class Builder<R extends Comparable<R>> {
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private CaptionBasedToString<R> captionBasedToString = (caption,number) -> caption;
        private CycleButton.TooltipSupplier<R> tooltipSupplier = (object)->ImmutableList.of();
        private R initialValue;
        private final Component caption;
        private final ValidateRange<R> validateRange;
        private final Consumer<R> onValueChanged;
        private Consumer<SliderButton<R>> onMouseOver = (btn)->{};
        private R step;

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

        public Builder<R> text(CaptionBasedToString<R> textCalculate){
            this.captionBasedToString = textCalculate;
            return this;
        }

        public Builder<R> tooltip(CycleButton.TooltipSupplier<R> tooltipSupplier){
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public Builder<R> onMouseOver(Consumer<SliderButton<R>> onMouseOver){
            this.onMouseOver = onMouseOver;
            return this;
        }

        public Builder<R> initial(R number){
            this.initialValue = number;
            return this;
        }

        public Builder<R> step(R step){
            this.step = step;
            return this;
        }

        public SliderButton<R> build(){
            validateRange.setStep(step);
            return new SliderButton<R>(caption,x,y,width,height,tooltipSupplier,captionBasedToString,validateRange,initialValue, onMouseOver,onValueChanged);
        }
    }

    public interface CaptionBasedToString<T> {
        Component toString(Component component, T object);
    }
}
