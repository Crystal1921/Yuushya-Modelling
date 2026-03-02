package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
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

public class EngraveItemResult implements IEngraveResult {
    private final ItemStack resultItemStack;
    @Getter
    private final String name;

    public EngraveItemResult(String name, ShareUtils.ShareItemInformation itemInfo) {
        this.name = name;
        List<TransformItemData> transformDataList = new ArrayList<>();
        itemInfo.transferItems(transformDataList);
        resultItemStack = ItemRegistry.ITEM_BLOCK.get().getDefaultInstance();
        resultItemStack.setHoverName(Component.literal(name));
        saveItemBlockData(resultItemStack, transformDataList);
    }

    private static void saveItemBlockData(ItemStack itemStack, List<TransformItemData> transformDataList) {
        CompoundTag compoundTag = new CompoundTag();
        ITransformItemDataInventory.saveAdditional(compoundTag, transformDataList);
        BlockItem.setBlockEntityData(itemStack, BlockEntityRegistry.ITEM_BLOCK_ENTITY.get(), compoundTag);
    }

    public ItemStack getResultItem() {
        return resultItemStack;
    }

}
