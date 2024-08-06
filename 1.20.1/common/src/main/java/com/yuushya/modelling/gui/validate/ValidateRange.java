package com.yuushya.modelling.gui.validate;

import java.util.Optional;

public interface ValidateRange<T> {
    Optional<T> validateValue(T value);
    double toSliderValue(T value);
    T fromSliderValue(double sliderValue);
}
