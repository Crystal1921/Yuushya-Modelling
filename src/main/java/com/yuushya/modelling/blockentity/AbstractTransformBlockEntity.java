package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.utils.VoxelShapeSerializer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for transform block entities that provides common functionality
 * for display and axis control.
 */
public abstract class AbstractTransformBlockEntity extends BlockEntity {

    protected Integer slot = 0;

    // Display control methods
    // Display control fields with timers
    @Getter
    protected boolean showFrame = false;
    protected int frameDisplayTimer = 0;

    @Setter
    @Getter
    protected boolean showText = false;
    protected int textDisplayTimer = 0;

    // Axis highlight control
    @Getter
    protected boolean showAxis = false;
    @Getter
    protected Direction.Axis highlightedAxis = null;
    protected int axisDisplayTimer = 0;
    @Getter
    @Setter
    protected VoxelShape customShape = Shapes.empty();

    public AbstractTransformBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    // Slot management
    public int getSlot() {
        return slot;
    }

    public abstract void setSlot(int slot);

    public abstract void writeBlockState(ItemStack itemStack, BlockState blockState);

    public void setShowFrame(boolean show) {
        this.showFrame = show;
    }

    public void triggerShowFrame() {
        this.showFrame = true;
        this.frameDisplayTimer = 5; // Display for 5 ticks
    }

    public void triggerShowText() {
        this.showText = true;
        this.textDisplayTimer = 5; // Display for 5 ticks
    }

    /**
     * Highlight a specific axis for display
     * @param axis The axis to highlight (X, Y, or Z)
     */
    public void highlightAxis(Direction.Axis axis) {
        this.highlightedAxis = axis;
        this.showAxis = true;
        this.axisDisplayTimer = 5; // Display for 5 ticks
    }

    /**
     * Enable axis display without highlighting a specific axis
     */
    public void triggerShowAxis() {
        this.showAxis = true;
        this.axisDisplayTimer = 5;
    }

    // Block entity synchronization
    public void saveChanged() {
        this.setChanged();
        if (this.getLevel() != null && !this.getLevel().isClientSide()) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compoundTag = super.getUpdateTag(registries);
        compoundTag.putByte("ControlSlot", slot.byteValue());
        return compoundTag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("ControlSlot", slot.byteValue());
        output.store("CustomShape", CompoundTag.CODEC, VoxelShapeSerializer.serializeVoxelShape(customShape));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        slot = (int) input.getByteOr("ControlSlot",(byte) 0);
        input.read("CustomShape", CompoundTag.CODEC).ifPresent(compoundTag -> {
            customShape = VoxelShapeSerializer.deserializeVoxelShape(compoundTag);
        });

        // Client chunk update
        if (this.getLevel() != null && this.getLevel().isClientSide()) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, AbstractTransformBlockEntity blockEntity) {
        // Handle frame display timer
        if (blockEntity.frameDisplayTimer > 0) {
            blockEntity.frameDisplayTimer--;
            if (blockEntity.frameDisplayTimer <= 0) {
                blockEntity.showFrame = false;
            }
        }

        // Handle text display timer
        if (blockEntity.textDisplayTimer > 0) {
            blockEntity.textDisplayTimer--;
            if (blockEntity.textDisplayTimer <= 0) {
                blockEntity.showText = false;
            }
        }

        // Handle axis display timer
        if (blockEntity.axisDisplayTimer > 0) {
            blockEntity.axisDisplayTimer--;
            if (blockEntity.axisDisplayTimer <= 0) {
                blockEntity.showAxis = false;
                blockEntity.highlightedAxis = null;
            }
        }
    }
}