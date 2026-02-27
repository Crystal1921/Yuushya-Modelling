package com.yuushya.modelling.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for transform block entities that provides common functionality
 * for display and axis control.
 */
public abstract class AbstractTransformBlockEntity extends BlockEntity {

    protected Integer slot = 0;

    // Display control fields
    protected Integer showFrame = 0;
    protected Integer showRotAxis = 0;
    protected Integer showPosAxis = 0;
    protected Integer showText = 0;

    @Setter
    @Getter
    protected Direction.Axis showAxis = null;

    public AbstractTransformBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    // Slot management
    public int getSlot() {
        return slot;
    }

    public abstract void setSlot(int slot);

    public abstract void writeBlockState(ItemStack itemStack, BlockState blockState);

    // Display control methods
    public boolean showFrame() {
        return showFrame > 0;
    }

    public void setShowFrame() {
        showFrame = 5;
    }

    public void consumeShowFrame() {
        showFrame = showFrame < 0 ? 0 : showFrame - 1;
    }

    public boolean showRotAxis() {
        return showRotAxis > 0;
    }

    public void setShowRotAxis() {
        showRotAxis = 5;
    }

    public boolean showPosAxis() {
        return showPosAxis > 0;
    }

    public void setShowPosAxis() {
        showPosAxis = 5;
    }

    public boolean showText() {
        return showText > 0;
    }

    public void setShowText() {
        showText = 5;
    }

    public void consumeShowAxis() {
        if (showRotAxis <= 0 && showPosAxis <= 0) {
            showAxis = null;
        }
    }

    public void consumeShow() {
        showRotAxis = showRotAxis < 0 ? 0 : showRotAxis - 1;
        showPosAxis = showPosAxis < 0 ? 0 : showPosAxis - 1;
        showText = showText < 0 ? 0 : showText - 1;
    }

    // Block entity synchronization
    public void saveChanged() {
        this.setChanged();
        if (this.getLevel() != null && !this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, access) -> {
            CompoundTag compoundTag = getUpdateTag(access);
            saveAdditional(compoundTag, access);
            return compoundTag;
        });
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compoundTag = super.getUpdateTag(registries);
        compoundTag.putByte("ControlSlot", slot.byteValue());
        return compoundTag;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        compoundTag.putByte("ControlSlot", slot.byteValue());
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        slot = (int) compoundTag.getByte("ControlSlot");

        // Client chunk update
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
}