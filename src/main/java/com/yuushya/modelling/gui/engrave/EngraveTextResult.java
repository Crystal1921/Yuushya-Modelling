package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.item.showblocktool.DestroyItem.saveToItem;

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
        saveToItem(resultItemStack, transformDataList);
    }

    @Override
    public ItemStack getResultItem() {
        return this.resultItemStack;
    }
}
