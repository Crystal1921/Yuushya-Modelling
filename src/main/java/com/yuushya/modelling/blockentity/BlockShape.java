package com.yuushya.modelling.blockentity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public enum BlockShape implements StringRepresentable {
    NONE(Shapes.empty()),
    FENCE(Shapes.create(0.4375, 0, 0.4375, 0.5625, 1.5, 0.5625)),
    BOTTOM_HALF(Shapes.create(0, 0, 0, 1, 0.5, 1)),
    TOP_HALF(Shapes.create(0, 0.5, 0, 1, 1, 1)),
    BLOCK(Shapes.block()),
    CUSTOM(Shapes.empty()),;

    public final VoxelShape voxelShape;

    BlockShape(VoxelShape voxelShape) {
        this.voxelShape = voxelShape;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }

    public Component getSymbol() {
        return Component.translatable("gui.showBlockScreen.shape." + name().toLowerCase());
    }

    /**
     * 找出与该形状完全一致的预设，用于把手工雕出的形状退化回预设。
     * 比较时只看体积是否相同，不要求内部栅格划分一致。
     *
     * @return 匹配的预设，没有匹配时返回 {@link #CUSTOM}
     */
    public static BlockShape presetOf(VoxelShape shape) {
        if (shape == null) {
            return CUSTOM;
        }
        for (BlockShape preset : values()) {
            if (preset != CUSTOM && sameShape(preset.voxelShape, shape)) {
                return preset;
            }
        }
        return CUSTOM;
    }

    /** 对称差为空即视为同一形状，不要求两者的内部栅格划分一致。 */
    private static boolean sameShape(VoxelShape first, VoxelShape second) {
        return !Shapes.joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
    }
}
