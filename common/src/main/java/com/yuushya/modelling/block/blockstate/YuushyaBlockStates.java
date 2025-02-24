package com.yuushya.modelling.block.blockstate;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class YuushyaBlockStates {
    public static final IntegerProperty LIT = IntegerProperty.create("lit",0,15);
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance",0,15);

    public static <T extends Comparable<T>> BlockState cycleState(BlockState blockState, Property<T> property, boolean doGetPre) {
        return blockState.setValue(property, getRelative(property.getPossibleValues(), blockState.getValue(property), doGetPre));
    }

    public static <T> T getRelative(Iterable<T> iterable, @Nullable T object, boolean doGetPre) {
        return doGetPre ? Util.findPreviousInIterable(iterable, object) : Util.findNextInIterable(iterable, object);
    }
}
