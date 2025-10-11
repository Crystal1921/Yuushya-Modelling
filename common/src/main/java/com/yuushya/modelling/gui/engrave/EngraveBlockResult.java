package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.item.showblocktool.DestroyItem.saveToItem;

public class EngraveBlockResult implements IEngraveResult {
    private final ItemStack resultItemStack;
    @Getter
    private final String name;

    public EngraveBlockResult(String name, ShareUtils.ShareBlockInformation itemInfo) {
        this(name, itemInfo, Minecraft.getInstance().level);
    }

    public EngraveBlockResult(String name, ShareUtils.ShareBlockInformation itemInfo, ClientLevel level) {
        this.name = name;
        List<TransformBlockData> transformDataList = new ArrayList<>();
        itemInfo.transfer(transformDataList);
        resultItemStack = YuushyaRegistries.ITEMS.get("showblock").get().getDefaultInstance();
        resultItemStack.set(DataComponents.ITEM_NAME, Component.literal(name));
        if (level != null) {
            saveToItem(resultItemStack, transformDataList, level.registryAccess());
        }
    }

    public ItemStack getResultItem() {
        return resultItemStack;
    }

}
