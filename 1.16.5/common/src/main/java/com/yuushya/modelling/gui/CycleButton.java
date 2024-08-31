//mojang official mapping 1.18.2
//all right reserved

package com.yuushya.modelling.gui;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.yuushya.modelling.gui.TooltipUtils.TooltipSupplier;

public class CycleButton<T> extends AbstractButton implements TooltipAccessor {
    static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = Screen::hasAltDown;
    private static final List<Boolean> BOOLEAN_OPTIONS;
    private final Component name;
    private int index;
    private T value;
    private final ValueListSupplier<T> values;
    private final Function<T, Component> valueStringifier;
    private final Function<CycleButton<T>, MutableComponent> narrationProvider;
    private final OnValueChange<T> onValueChange;
    private final TooltipSupplier<T> tooltipSupplier;
    private final boolean displayOnlyValue;

    CycleButton(int x, int y, int width, int height, Component message, Component name, int index, T value, ValueListSupplier<T> values, Function<T, Component> valueStringifier, Function<CycleButton<T>, MutableComponent> narrationProvider, OnValueChange<T> onValueChange, TooltipSupplier<T> tooltipSupplier, boolean displayOnlyValue) {
        super(x, y, width, height, message);
        this.name = name;
        this.index = index;
        this.value = value;
        this.values = values;
        this.valueStringifier = valueStringifier;
        this.narrationProvider = narrationProvider;
        this.onValueChange = onValueChange;
        this.tooltipSupplier = tooltipSupplier;
        this.displayOnlyValue = displayOnlyValue;
    }

    public void onPress() {
        if (Screen.hasShiftDown()) {
            this.cycleValue(-1);
        } else {
            this.cycleValue(1);
        }

    }

    private void cycleValue(int delta) {
        List<T> list = this.values.getSelectedList();
        this.index = Mth.positiveModulo(this.index + delta, list.size());
        T object = list.get(this.index);
        this.updateValue(object);
        this.onValueChange.onValueChange(this, object);
    }

    private T getCycledValue(int delta) {
        List<T> list = this.values.getSelectedList();
        return list.get(Mth.positiveModulo(this.index + delta, list.size()));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0.0) {
            this.cycleValue(-1);
        } else if (delta < 0.0) {
            this.cycleValue(1);
        }

        return true;
    }

    public void setValue(T value) {
        List<T> list = this.values.getSelectedList();
        int i = list.indexOf(value);
        if (i != -1) {
            this.index = i;
        }

        this.updateValue(value);
    }

    private void updateValue(T value) {
        Component component = this.createLabelForValue(value);
        this.setMessage(component);
        this.value = value;
    }

    private Component createLabelForValue(T value) {
        return this.displayOnlyValue ? this.valueStringifier.apply(value) : this.createFullName(value);
    }

    private MutableComponent createFullName(T value) {
        return optionNameValue(this.name, this.valueStringifier.apply(value));
    }

    public T getValue() {
        return this.value;
    }

    protected MutableComponent createNarrationMessage() {
        return this.narrationProvider.apply(this);
    }

    public MutableComponent createDefaultNarrationMessage() {
        return wrapDefaultNarrationMessage(this.displayOnlyValue ? this.createFullName(this.value) : this.getMessage());
    }

    public Optional<List<FormattedCharSequence>> getTooltip() {
        return Optional.ofNullable(this.tooltipSupplier.apply(this.value));
    }

    public static <T> Builder<T> builder(Function<T, Component> valueStringifier) {
        return new Builder<T>(valueStringifier);
    }

    public static Builder<Boolean> booleanBuilder(Component componentOn, Component componentOff) {
        return (new Builder<Boolean>((boolean_) -> {
            return boolean_ ? componentOn : componentOff;
        })).withValues(BOOLEAN_OPTIONS);
    }

    public static Builder<Boolean> onOffBuilder() {
        return (new Builder<Boolean>((boolean_) -> {
            return boolean_ ? OPTION_ON : OPTION_OFF;
        })).withValues(BOOLEAN_OPTIONS);
    }

    public static Builder<Boolean> onOffBuilder(boolean initialValue) {
        return onOffBuilder().withInitialValue(initialValue);
    }

    static {
        BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
    }

    private interface ValueListSupplier<T> {
        List<T> getSelectedList();

        List<T> getDefaultList();

        static <T> ValueListSupplier<T> create(List<T> values) {
            final List<T> list = ImmutableList.copyOf(values);
            return new ValueListSupplier<T>() {
                public List<T> getSelectedList() {
                    return list;
                }

                public List<T> getDefaultList() {
                    return list;
                }
            };
        }

        static <T> ValueListSupplier<T> create(final BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
            final List<T> list = ImmutableList.copyOf(defaultList);
            final List<T> list2 = ImmutableList.copyOf(selectedList);
            return new ValueListSupplier<T>() {
                public List<T> getSelectedList() {
                    return altListSelector.getAsBoolean() ? list2 : list;
                }

                public List<T> getDefaultList() {
                    return list;
                }
            };
        }
    }

    public interface OnValueChange<T> {
        void onValueChange(CycleButton cycleButton, T object);
    }

    public static class Builder<T> {
        private int initialIndex;
        @Nullable
        private T initialValue;
        private final Function<T, Component> valueStringifier;
        private TooltipSupplier<T> tooltipSupplier = (object) -> {
            return ImmutableList.of();
        };
        private Function<CycleButton<T>, MutableComponent> narrationProvider = CycleButton::createDefaultNarrationMessage;
        private ValueListSupplier<T> values = CycleButton.ValueListSupplier.create(ImmutableList.of());
        private boolean displayOnlyValue;

        public Builder(Function<T, Component> valueStringifier) {
            this.valueStringifier = valueStringifier;
        }

        public Builder<T> withValues(List<T> values) {
            this.values = CycleButton.ValueListSupplier.create(values);
            return this;
        }

        @SafeVarargs
        public final Builder<T> withValues(T... values) {
            return this.withValues(ImmutableList.copyOf(values));
        }

        public Builder<T> withValues(List<T> defaultList, List<T> selectedList) {
            this.values = CycleButton.ValueListSupplier.create(CycleButton.DEFAULT_ALT_LIST_SELECTOR, defaultList, selectedList);
            return this;
        }

        public Builder<T> withValues(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
            this.values = CycleButton.ValueListSupplier.create(altListSelector, defaultList, selectedList);
            return this;
        }

        public Builder<T> withTooltip(TooltipSupplier<T> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public Builder<T> withInitialValue(T initialValue) {
            this.initialValue = initialValue;
            int i = this.values.getDefaultList().indexOf(initialValue);
            if (i != -1) {
                this.initialIndex = i;
            }

            return this;
        }

        public Builder<T> withCustomNarration(Function<CycleButton<T>, MutableComponent> narrationProvider) {
            this.narrationProvider = narrationProvider;
            return this;
        }

        public Builder<T> displayOnlyValue() {
            this.displayOnlyValue = true;
            return this;
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name) {
            return this.create(x, y, width, height, name, (cycleButton, object) -> {
            });
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name, OnValueChange<T> onValueChange) {
            List<T> list = this.values.getDefaultList();
            if (list.isEmpty()) {
                throw new IllegalStateException("No values for cycle button");
            } else {
                T object = this.initialValue != null ? this.initialValue : list.get(this.initialIndex);
                Component component = this.valueStringifier.apply(object);
                Component component2 = this.displayOnlyValue ? component : optionNameValue(name, component);
                return new CycleButton<T>(x, y, width, height, component2, name, this.initialIndex, object, this.values, this.valueStringifier, this.narrationProvider, onValueChange, this.tooltipSupplier, this.displayOnlyValue);
            }
        }
    }

    public static MutableComponent wrapDefaultNarrationMessage(Component message) {
        return new TranslatableComponent("gui.narrate.button", message);
    }
    public static MutableComponent optionNameValue(Component caption, Component valueMessage) {
        return new TranslatableComponent("options.generic_value", caption, valueMessage);
    }

    public static final Component OPTION_ON = new TranslatableComponent("options.on");
    public static final Component OPTION_OFF = new TranslatableComponent("options.off");
}