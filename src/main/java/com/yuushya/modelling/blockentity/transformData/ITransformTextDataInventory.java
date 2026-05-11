package com.yuushya.modelling.blockentity.transformData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ITransformTextDataInventory {
    //利用函数式构造参数类型的包装器，保证返回的实例与原来相同 Creates an inventory from the text data list. Must return the same instance every time it's called.
    static ITransformTextDataInventory of(List<TransformTextData> transformDatas) {
        return () -> transformDatas;
    }//interface ::=functional

    //作为lambda类型的唯一抽象方法 the unique abstract function of interface and provide the lambda type.
    List<TransformTextData> getTransformData();

    //default function for control the inventory
    default int size() {
        return getTransformData().size();
    }

    default boolean isEmpty() {
        for (TransformTextData transformData : getTransformData()) {
            if (!transformData.textLines.isEmpty()) return false;
        }
        return true;
    }

    @NotNull
    default TransformTextData getTransformData(int slot) {
        if (slot < size())
            return getTransformData().get(slot);
        else
            return getTransformData().get(Math.min(size() - 1, 0));
    }

    default void addTransformData(int slot, TransformTextData transformData) {
        if (slot < size())
            getTransformData().add(slot, transformData);
        else
            getTransformData().add(transformData);
    }

    default void addTransformData(TransformTextData transformData) {
        getTransformData().add(transformData);
    }

    default void removeTransformData(int slot) {
        getTransformData(slot).set();
    }

    default void removeSlotTextLines(int slot) {
        getTransformData(slot).textLines.clear();
    }

    default void setTransformData(int slot, TransformTextData transformData) {
        getTransformData(slot).set(transformData);
    }

    default void setSlotTextLines(int slot, List<String> textLines) {
        getTransformData(slot).textLines = new java.util.ArrayList<>(textLines);
    }

    default void setSlotShown(int slot, boolean isShown) {
        getTransformData(slot).isShown = isShown;
    }

    default void clear() {
        getTransformData().clear();
    }
}
