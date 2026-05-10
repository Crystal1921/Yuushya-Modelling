package com.yuushya.modelling.blockentity.transformData;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ITransformTextDataInventory {
    //利用函数式构造参数类型的包装器，保证返回的实例与原来相同 Creates an inventory from the text data list. Must return the same instance every time it's called.
    static ITransformTextDataInventory of(List<TransformTextData> transformDatas) {
        return () -> transformDatas;
    }//interface ::=functional

    //readNbt from compoundTag
    static void load(CompoundTag compoundTag, List<TransformTextData> transformDatas) {
        ListTag listTag = compoundTag.getList("Blocks").orElse(new ListTag());
        if (!transformDatas.isEmpty()) transformDatas.clear();
        for (int index = 0; index < listTag.size(); index++) {
            TransformTextData transformData = new TransformTextData();
            CompoundTag compoundTagTemp = listTag.getCompound(index).orElse(new CompoundTag());
            transformData.load(compoundTagTemp);
            transformDatas.add(transformData);
        }
        if (transformDatas.isEmpty()) transformDatas.add(new TransformTextData());
    }

    //readNbt from ValueInput
    static void load(ValueInput input, List<TransformTextData> transformDatas) {
        input.read("Blocks", CompoundTag.CODEC).ifPresent(compoundTag -> {
            ListTag listTag = compoundTag.getList("Blocks").orElse(new ListTag());
            if (!transformDatas.isEmpty()) transformDatas.clear();
            for (int index = 0; index < listTag.size(); index++) {
                TransformTextData transformData = new TransformTextData();
                CompoundTag compoundTagTemp = listTag.getCompound(index).orElse(new CompoundTag());
                transformData.load(compoundTagTemp);
                transformDatas.add(transformData);
            }
            if (transformDatas.isEmpty()) transformDatas.add(new TransformTextData());
        });
    }

    //writeNbt to compoundTag
    static void saveAdditional(CompoundTag compoundTag, List<TransformTextData> transformDatas, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (TransformTextData transformData : transformDatas) {
            CompoundTag compoundTagTemp = new CompoundTag();
            compoundTagTemp.putByte("Slot", (byte) index);
            transformData.saveAdditional(compoundTagTemp, registries);
            listTag.add(compoundTagTemp);
            index++;
        }
        if (!listTag.isEmpty()) compoundTag.put("Blocks", listTag);
    }

    //writeNbt to ValueOutput
    static void saveAdditional(ValueOutput output, List<TransformTextData> transformDatas, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (TransformTextData transformData : transformDatas) {
            CompoundTag compoundTagTemp = new CompoundTag();
            compoundTagTemp.putByte("Slot", (byte) index);
            transformData.saveAdditional(compoundTagTemp, registries);
            listTag.add(compoundTagTemp);
            index++;
        }
        if (!listTag.isEmpty()) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("Blocks", listTag);
            output.store("Blocks", CompoundTag.CODEC, compoundTag);
        }
    }

    static void saveAdditionalWithoutEmpty(CompoundTag compoundTag, List<TransformTextData> transformDatas, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (TransformTextData transformData : transformDatas) {
            if (!transformData.textLines.isEmpty()) {
                CompoundTag compoundTagTemp = new CompoundTag();
                compoundTagTemp.putByte("Slot", (byte) index);
                transformData.saveAdditional(compoundTagTemp, registries);
                listTag.add(compoundTagTemp);
            }
            index++;
        }
        if (!listTag.isEmpty()) compoundTag.put("Blocks", listTag);
    }

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
