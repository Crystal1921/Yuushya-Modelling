package com.yuushya.modelling.blockentity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public enum BlockShape implements StringRepresentable {
    NONE(Shapes.empty()),
    FENCE(Shapes.create(0.4375, 0, 0.4375, 0.5625, 1, 0.5625)),
    BOTTOM_HALF(Shapes.create(0, 0, 0, 1, 0.5, 1)),
    TOP_HALF(Shapes.create(0, 0.5, 0, 1, 1, 1)),
    BLOCK(Shapes.block());

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
}
