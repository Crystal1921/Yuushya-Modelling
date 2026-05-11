package com.yuushya.modelling.blockentity.transformData;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ITransformDataInventory {
    //利用函数式构造参数类型的包装器，保证返回的实例与原来相同 Creates an inventory from the block list.Must return the same instance every time it's called.
    static ITransformDataInventory of(List<TransformBlockData> transformDatas) {
        return () -> transformDatas;
    }//interface ::=functional

    //writeNbt to ValueOutput
    static void saveAdditional(ValueOutput output, List<? extends ITransformDataProvider> transformDatas, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (ITransformDataProvider transformData : transformDatas) {
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

    //作为lambda类型的唯一抽象方法 the unique abstract function of interface and provide the lambda type.
    List<TransformBlockData> getTransformData();

    //default function for control the inventory
    default int size() {
        return getTransformData().size();
    }

    default boolean isEmpty() {
        for (TransformBlockData transformData : getTransformData()) {
            if (!(transformData.blockState.getBlock() instanceof AirBlock)) return false;
        }
        return true;
    }

    @NotNull
    default TransformBlockData getTransformData(int slot) {
        if (slot < size())
            return getTransformData().get(slot);
        else
            return getTransformData().get(Math.min(size() - 1, 0));
    }

    default void addTransformData(int slot, TransformBlockData transformData) {
        if (slot < size())
            getTransformData().add(slot, transformData);
        else
            getTransformData().add(transformData);
    }

    default void addTransformData(TransformBlockData transformData) {
        getTransformData().add(transformData);
    }

    default void removeTransformData(int slot) {
        getTransformData(slot).set();
    }

    default void removeSlotBlockState(int slot) {
        getTransformData(slot).blockState = Blocks.AIR.defaultBlockState();
    }

    default void setTransformData(int slot, TransformBlockData transformData) {
        getTransformData(slot).set(transformData);
    }

    default void setSlotBlockState(int slot, BlockState blockState) {
        getTransformData(slot).blockState = blockState;
    }

    default void setSlotShown(int slot, boolean isShown) {
        getTransformData(slot).isShown = isShown;
    }

    default void clear() {
        getTransformData().clear();
    }
}
