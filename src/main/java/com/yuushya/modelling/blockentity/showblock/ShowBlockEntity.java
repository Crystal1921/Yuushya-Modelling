package com.yuushya.modelling.blockentity.showblock;


import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.gui.engrave.EngraveBlockResult;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.LIT;
import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
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

    @Override
    //readNbt
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        ITransformDataInventory.load(compoundTag, transformData);
    }

    @Override
    //writeNbt
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        ITransformDataInventory.saveAdditional(compoundTag, transformData, registries);
    }

    @Override
    //toInitialChunkDataNbt //When you first load world it writeNbt firstly
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compoundTag = super.getUpdateTag(registries);
        ITransformDataInventory.saveAdditional(compoundTag, transformData, registries);
        return compoundTag;
    }

    public void writeBlockState(ItemStack itemStack, BlockState blockState) {
        BlockItemStateProperties blockItemStateProperties = BlockItemStateProperties.EMPTY;
        itemStack.set(DataComponents.BLOCK_STATE, blockItemStateProperties
                .with(LIT, blockState.getValue(LIT))
                .with(SHAPES, blockState.getValue(SHAPES)));
    }

    public void setRemoved() {
        if (!this.isEmpty()) {
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





