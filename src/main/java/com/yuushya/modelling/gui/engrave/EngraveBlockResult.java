package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
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
        this.name = name;
        List<TransformBlockData> transformDataList = new ArrayList<>();
        itemInfo.transfer(transformDataList);
        resultItemStack = ItemRegistry.SHOW_BLOCK.get().getDefaultInstance();
        resultItemStack.setHoverName(Component.literal(name));
        saveToItem(resultItemStack, transformDataList);

    }

    public ItemStack getResultItem() {
        return resultItemStack;
    }

}
