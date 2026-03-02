package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EngraveBlockResult implements IEngraveResult {
    private final ItemStack resultItemStack;
    @Getter
    private final String name;

    public EngraveBlockResult(String name, ShareUtils.ShareBlockInformation itemInfo) {
        this.name = name;
        List<TransformBlockData> transformDataList = new ArrayList<>();
        itemInfo.transfer(transformDataList);
        resultItemStack = ItemRegistry.SHOW_BLOCK.get().getDefaultInstance();
        resultItemStack.setHoverName(Component.literal(name));
        saveShowBlockData(resultItemStack, transformDataList);
    }

    private static void saveShowBlockData(ItemStack itemStack, List<TransformBlockData> transformDataList) {
        CompoundTag compoundTag = new CompoundTag();
        ITransformDataInventory.saveAdditional(compoundTag, transformDataList);
        BlockItem.setBlockEntityData(itemStack, BlockEntityRegistry.SHOW_BLOCK_ENTITY.get(), compoundTag);
    }

    public ItemStack getResultItem() {
        return resultItemStack;
    }

}
