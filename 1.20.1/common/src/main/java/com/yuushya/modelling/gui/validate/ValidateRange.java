package com.yuushya.modelling.gui.validate;

import java.util.Optional;

interface ValidateSet<T>{
    Optional<T> validateValue(T value);
    T defaultValue();
}
interface Range<T extends Comparable<T>>{
    T minInclusive();
    T maxInclusive();
}
interface StepRange<T extends Comparable<T>> extends Range<T>{
    void setStep(double step);
}
interface SliderValueSet<T> {
    double toSliderValue(T value);
    T fromSliderValue(double sliderValue);
}
public interface ValidateRange<T extends Comparable<T>> extends ValidateSet<T>,StepRange<T>,SliderValueSet<T>{
    @Override
    default T defaultValue(){ return this.minInclusive(); }

    @Override
    default Optional<T> validateValue(T value){
        if(value.compareTo(this.minInclusive()) < 0 ) return Optional.of(this.minInclusive());
        if(value.compareTo(this.maxInclusive()) > 0 ) return Optional.of(this.maxInclusive());
        return Optional.of(value);
    }
}

