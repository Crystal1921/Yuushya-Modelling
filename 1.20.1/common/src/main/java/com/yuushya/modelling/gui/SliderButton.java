package com.yuushya.modelling.gui;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.gui.validate.ValidateRange;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Consumer;

public class SliderButton<T> extends AbstractSliderButton {
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

    public static class Builder<R> {
        private final Component message;
        private final Consumer<R> onValueChanged;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        public Builder(Component component, Consumer<R> onValueChanged) {
            this.message = component;
            this.onValueChanged = onValueChanged;
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

    }

}
