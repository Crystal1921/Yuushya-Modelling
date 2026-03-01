package com.yuushya.modelling.blockentity.showblock;


import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.gui.engrave.EngraveBlockResult;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import com.yuushya.modelling.utils.YuushyaDataTags;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.LIT;
import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.item.showblocktool.HistoryItem.HISTORY_SHOWBLOCK_MAP;

public class ShowBlockEntity extends AbstractTransformBlockEntity implements ITransformDataInventory {

    @Getter
    private final List<TransformBlockData> transformData;

    public ShowBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityRegistry.SHOW_BLOCK_ENTITY.get(), blockPos, blockState);
        transformData = new ArrayList<>();
        transformData.add(new TransformBlockData());
        slot = 0;
    }

    @NotNull
    public TransformBlockData getTransFormDataNow() {
        return getTransformData(slot);
    }

    public void removeTransFormDataNow() {
        removeTransformData(slot);
    }

    public void setTransformDataNow(TransformBlockData transformData) {
        setTransformData(slot, transformData);
    }

    public void setSlotBlockStateNow(BlockState blockState) {
        setSlotBlockState(slot, blockState);
    }

    @Override
    public void setSlot(int slot) {
        if (slot >= transformData.size()) {
            for (int i = slot - transformData.size() + 1; i > 0; i--)
                transformData.add(new TransformBlockData());
        }
        this.slot = slot;
    }

    // Display helper fields and methods for client-side rendering
    private Integer showFrame = 0;
    public boolean showFrame() { return showFrame > 0; }
    public void setShowFrame() { showFrame = 5; }
    public void consumeShowFrame() {
        showFrame = showFrame < 0 ? 0 : showFrame - 1;
    }

    // 显示旋转的坐标轴
    private Integer showRotAxis = 0;
    public boolean showRotAxis() { return showRotAxis > 0; }
    public void setShowRotAixs() { showRotAxis = 5; }

    // 显示平移的坐标轴
    private Integer showPosAxis = 0;
    public boolean showPosAxis() { return showPosAxis > 0; }
    public void setShowPosAixs() { showPosAxis = 5; }

    private Integer showText = 0;
    public boolean showText() { return showText > 0; }
    public void setShowText() { showText = 5; }

    private Direction.Axis showAxis = null;
    public Direction.Axis getShowAxis() { return showAxis; }
    public void setShowAxis(Direction.Axis axis) { showAxis = axis; }
    public void consumeShowAxis() {
        if (showRotAxis <= 0 && showPosAxis <= 0) showAxis = null;
    }

    public void consumeShow() {
        showRotAxis = showRotAxis < 0 ? 0 : showRotAxis - 1;
        showPosAxis = showPosAxis < 0 ? 0 : showPosAxis - 1;
        showText = showText < 0 ? 0 : showText - 1;
    }

    @Override
    //readNbt
    //called on server chunk loaded or client received block entity data packet
    public void load(@NotNull CompoundTag compoundTag) {
        super.load(compoundTag);
        ITransformDataInventory.load(compoundTag, transformData);
        slot = (int) compoundTag.getByte("ControlSlot");

        //client chunk update
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    //writeNbt
    protected void saveAdditional(@NotNull CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ITransformDataInventory.saveAdditional(compoundTag, transformData);
        compoundTag.putByte("ControlSlot", slot.byteValue());
    }

    @Override
    //toInitialChunkDataNbt //When you first load world it writeNbt firstly
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag compoundTag = super.getUpdateTag();
        ITransformDataInventory.saveAdditional(compoundTag, transformData);
        return compoundTag;
    }

    public void saveChanged() {
        this.setChanged();

        if (this.getLevel() != null && !this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag compoundTag = getUpdateTag();
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity) -> {
            saveAdditional(compoundTag);
            return compoundTag;
        });
    }

    public void writeBlockState(ItemStack itemStack, BlockState blockState) {
        YuushyaDataTags.setLit(itemStack, blockState.getValue(LIT));
        YuushyaDataTags.setShapes(itemStack, blockState.getValue(SHAPES).getSerializedName());
        YuushyaDataTags.setEnableAO(itemStack, blockState.getValue(ENABLE_AO));
    }

    public void setRemoved() {
        if (this.level != null && !this.isEmpty() && this.level.isClientSide) {
            String res = ShareUtils.transfer(this.getTransformData());
            ShareUtils.ShareBlockInformation information = ShareUtils.from(res);
            if (this.level != null) {
                String name = this.level.dimension().location() + "/" + this.getBlockPos().toShortString();
                HISTORY_SHOWBLOCK_MAP.put(name, new EngraveBlockResult(name, information));
            }
        }
        super.setRemoved();
    }
}





