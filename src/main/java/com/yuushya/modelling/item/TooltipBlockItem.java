package com.yuushya.modelling.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class TooltipBlockItem extends BlockItem {
    int tipLines;
    public TooltipBlockItem(Block block, Properties properties, int tipLines) {
        super(block, properties);
        this.tipLines = tipLines;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        for (int i = 1; i <= tipLines; i++)
            builder.accept(Component.translatable(this.getDescriptionId() + ".line" + i));
    }
}
