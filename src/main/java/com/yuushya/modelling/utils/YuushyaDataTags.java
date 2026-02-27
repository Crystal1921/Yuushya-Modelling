package com.yuushya.modelling.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class YuushyaDataTags {
    public static final String TRANS_DIRECTION = "TransDirection";
    public static final String BLOCKSTATE = "BlockState";
    public static final String TRANSFORM_DATA = "TransformData";
    public static final String COLOR_DATA = "ColorData";

    // Trans Direction (Integer)
    public static int getTransDirection(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt(TRANS_DIRECTION);
    }

    public static void setTransDirection(ItemStack itemStack, int value) {
        itemStack.getOrCreateTag().putInt(TRANS_DIRECTION, value);
    }

    // BlockState
    public static BlockState getBlockState(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.contains(BLOCKSTATE)) {
            return BlockState.CODEC.decode(NbtOps.INSTANCE, tag.get(BLOCKSTATE))
                    .result()
                    .map(Pair::getFirst)
                    .orElse(null);
        }
        return null;
    }

    public static void setBlockState(ItemStack itemStack, BlockState blockState) {
        if (blockState == null) {
            itemStack.getOrCreateTag().remove(BLOCKSTATE);
            return;
        }
        CompoundTag tag = itemStack.getOrCreateTag();
        BlockState.CODEC.encodeStart(NbtOps.INSTANCE, blockState)
                .result()
                .ifPresent(nbt -> tag.put(BLOCKSTATE, nbt));
    }

    // Transform Data (CompoundTag)
    public static CompoundTag getTransformData(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.contains(TRANSFORM_DATA)) {
            return tag.getCompound(TRANSFORM_DATA);
        }
        return new CompoundTag();
    }

    public static void setTransformData(ItemStack itemStack, CompoundTag transformData) {
        itemStack.getOrCreateTag().put(TRANSFORM_DATA, transformData);
    }

    // Color Data (Integer)
    public static int getColorData(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt(COLOR_DATA);
    }

    public static void setColorData(ItemStack itemStack, int value) {
        itemStack.getOrCreateTag().putInt(COLOR_DATA, value);
    }

    public static boolean hasColorData(ItemStack itemStack) {
        return itemStack.getOrCreateTag().contains(COLOR_DATA);
    }
}
