package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.ITransformTextDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
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

public class EngraveTextResult implements IEngraveResult {
    @Getter
    private final String name;
    private final ItemStack resultItemStack;

    public EngraveTextResult(String name, ShareUtils.SharedTextInformation itemInfo) {
        this.name = name;
        List<TransformTextData> transformDataList = new ArrayList<>();
        itemInfo.transferTexts(transformDataList);
        resultItemStack = ItemRegistry.TEXT_BLOCK.get().getDefaultInstance();
        resultItemStack.setHoverName(Component.literal(name));
        saveTextBlockData(resultItemStack, transformDataList);
    }

    private static void saveTextBlockData(ItemStack itemStack, List<TransformTextData> transformDataList) {
        CompoundTag compoundTag = new CompoundTag();
        ITransformTextDataInventory.saveAdditional(compoundTag, transformDataList);
        BlockItem.setBlockEntityData(itemStack, BlockEntityRegistry.TEXT_BLOCK_ENTITY.get(), compoundTag);
    }

    @Override
    public ItemStack getResultItem() {
        return this.resultItemStack;
    }
}
