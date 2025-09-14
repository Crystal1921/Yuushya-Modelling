package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemBlockEntity extends AbstractTransformBlockEntity implements ITransformItemDataInventory {
    @Getter
    private final List<TransformItemData> transformData;
    
    @NotNull
    public TransformItemData getTransFormDataNow(){return getTransformData(slot);}
    public void removeTransFormDataNow(){removeTransformData(slot);}
    public void setTransformDataNow(TransformItemData transformData){setTransformData(slot,transformData);}
    public void setSlotBlockStateNow(ItemStack itemStack){setSlotBlockState(slot,itemStack);}

    @Override
    public void setSlot(int slot){
        if (slot>= transformData.size()){
            for (int i = slot- transformData.size()+1; i>0; i--)
                transformData.add(new TransformItemData());
        }
        this.slot=slot;
    }

    public ItemBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(YuushyaRegistries.ITEM_BLOCK_ENTITY.get(), blockPos, blockState);
        transformData = new ArrayList<>();
        transformData.add(new TransformItemData());
        slot = 0;
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        ITransformItemDataInventory.load(compoundTag, transformData, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        ITransformItemDataInventory.saveAdditional(compoundTag, transformData, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compoundTag = super.getUpdateTag(registries);
        ITransformItemDataInventory.saveAdditional(compoundTag, transformData, registries);
        return compoundTag;
    }
}
