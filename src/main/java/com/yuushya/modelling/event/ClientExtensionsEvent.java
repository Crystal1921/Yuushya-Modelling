package com.yuushya.modelling.event;

import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.item.GetBlockStateItemForge;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ClientExtensionsEvent {
    @SubscribeEvent
    public static void RegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(TextBlock.ITEM_EXTENSIONS, ItemRegistry.TEXT_BLOCK);
        event.registerItem(GetBlockStateItemForge.ITEM_EXTENSIONS, ItemRegistry.GET_BLOCKSTATE_ITEM);
    }
}
