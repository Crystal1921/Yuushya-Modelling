package com.yuushya.modelling.item;

import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.registries.BlockRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class TextBlockItem extends BlockItem {
    public TextBlockItem() {
        super(BlockRegistry.TEXT_BLOCK.get(), new Item.Properties());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(TextBlock.ITEM_EXTENSIONS);
    }
}
