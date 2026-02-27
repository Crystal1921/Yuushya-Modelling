package com.yuushya.modelling.blockentity.transformData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ITransformItemDataInventory {
    //利用函数式构造参数类型的包装器，保证返回的实例与原来相同 Creates an inventory from the block list.Must return the same instance every time it's called.
    static ITransformItemDataInventory of(List<TransformItemData> transformDatas) {
        return () -> transformDatas;
    }//interface ::=functional

    //readNbt from compoundTag
    static void load(CompoundTag compoundTag, List<TransformItemData> transformDatas) {
        ListTag listTag = compoundTag.getList("Blocks", 10);//int index=0;//10 means Compound
        if (!transformDatas.isEmpty()) transformDatas.clear();
        for (int index = 0; index < listTag.size(); index++) {
            TransformItemData transformData = new TransformItemData();
            CompoundTag compoundTagTemp = listTag.getCompound(index);
            transformData.load(compoundTagTemp);
            transformDatas.add(transformData);
        }
        if (transformDatas.isEmpty()) transformDatas.add(new TransformItemData());
    }

    //writeNbt to compoundTag
    static void saveAdditional(CompoundTag compoundTag, List<TransformItemData> transformDatas) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (TransformItemData transformData : transformDatas) {
            //if(!(transformData.blockState.getBlock() instanceof AirBlock)){
            CompoundTag compoundTagTemp = new CompoundTag();
            compoundTagTemp.putByte("Slot", (byte) index);
            transformData.saveAdditional(compoundTagTemp);
            listTag.add(compoundTagTemp);
            //}
            index++;
        }
        if (!listTag.isEmpty()) compoundTag.put("Blocks", listTag);
    }

    static void saveAdditionalWithoutAir(CompoundTag compoundTag, List<TransformItemData> transformDatas) {
        ListTag listTag = new ListTag();
        int index = 0;
        for (TransformItemData transformData : transformDatas) {
            if (!(transformData.itemStack.isEmpty())) {
                CompoundTag compoundTagTemp = new CompoundTag();
                compoundTagTemp.putByte("Slot", (byte) index);
                transformData.saveAdditional(compoundTagTemp);
                listTag.add(compoundTagTemp);
            }
            index++;
        }
        if (!listTag.isEmpty()) compoundTag.put("Blocks", listTag);
    }

    //作为lambda类型的唯一抽象方法 the unique abstract function of interface and provide the lambda type.
    List<TransformItemData> getTransformData();

    //default function for control the inventory
    default int size() {
        return getTransformData().size();
    }

    default boolean isEmpty() {
        for (TransformItemData transformData : getTransformData()) {
            return transformData.itemStack.isEmpty();
        }
        return true;
    }

    @NotNull
    default TransformItemData getTransformData(int slot) {
        if (slot < size())
            return getTransformData().get(slot);
        else
            return getTransformData().get(Math.min(size() - 1, 0));
    }

    default void addTransformData(int slot, TransformItemData transformData) {
        if (slot < size())
            getTransformData().add(slot, transformData);
        else
            getTransformData().add(transformData);
    }

    default void addTransformData(TransformItemData transformData) {
        getTransformData().add(transformData);
    }

    default void removeTransformData(int slot) {
        getTransformData(slot).set();
    }

    default void removeSlotBlockState(int slot) {
        getTransformData(slot).itemStack = Items.AIR.getDefaultInstance();
    }

    default void setTransformData(int slot, TransformItemData transformData) {
        getTransformData(slot).set(transformData);
    }

    default void setSlotBlockState(int slot, ItemStack itemStack) {
        getTransformData(slot).itemStack = itemStack;
    }

    default void setSlotShown(int slot, boolean isShown) {
        getTransformData(slot).isShown = isShown;
    }

    default void clear() {
        getTransformData().clear();
    }
}
