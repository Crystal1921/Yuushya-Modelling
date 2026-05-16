package com.yuushya.modelling.event;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntityRender;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntityRender;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender;
import com.yuushya.modelling.gui.engrave.EngraveScreen;
import com.yuushya.modelling.gui.history.HistoryScreen;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.registries.MenuRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.EngraveMenu.get(), EngraveScreen::new);
        event.register(MenuRegistry.HistoryMenu.get(), HistoryScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHOW_BLOCK_ENTITY.get(), ShowBlockEntityRender::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.ITEM_BLOCK_ENTITY.get(), ItemBlockEntityRender::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.TEXT_BLOCK_ENTITY.get(), TextBlockEntityRender::new);
    }
}
