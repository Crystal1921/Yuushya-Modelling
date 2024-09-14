package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.ShareUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.item.showblocktool.DestroyItem.saveToItem;

public class EngraveItemResult {
    private final ItemStack resultItemStack;
    private final String name;
    public ItemStack getResultItem(){return resultItemStack;}
    public EngraveItemResult(String name, ShareUtils.ShareInformation itemInfo){
        this.name = name;
        List<TransformData> transformDataList = new ArrayList<>();
        itemInfo.transfer(transformDataList);
        resultItemStack = YuushyaRegistries.ITEMS.get("showblock").get().getDefaultInstance();
        resultItemStack.set(DataComponents.ITEM_NAME, Component.literal(name));
        saveToItem(resultItemStack,transformDataList);
    }
}
