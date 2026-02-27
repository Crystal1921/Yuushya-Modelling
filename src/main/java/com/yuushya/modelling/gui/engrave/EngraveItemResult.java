package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.item.showblocktool.DestroyItem.saveToItem;

public class EngraveItemResult implements IEngraveResult {
    private final ItemStack resultItemStack;
    @Getter
    private final String name;

    public EngraveItemResult(String name, ShareUtils.ShareItemInformation itemInfo) {
        this.name = name;
        List<TransformItemData> transformDataList = new ArrayList<>();
        itemInfo.transferItems(transformDataList);
        resultItemStack = ItemRegistry.ITEM_BLOCK.get().getDefaultInstance();
        resultItemStack.set(DataComponents.ITEM_NAME, Component.literal(name));
        if (registryAccess != null) {
            saveToItem(resultItemStack, transformDataList);
        }
    }

    public ItemStack getResultItem() {
        return resultItemStack;
    }

}
