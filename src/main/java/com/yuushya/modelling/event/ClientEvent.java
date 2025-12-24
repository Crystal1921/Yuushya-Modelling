package com.yuushya.modelling.event;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntityRender;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntityRender;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender;
import com.yuushya.modelling.gui.engrave.EngraveScreen;
import com.yuushya.modelling.gui.history.HistoryScreen;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.registries.MenuRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

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

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (String s : List.of("rot_trans_item", "pos_trans_item", "micro_pos_trans_item", "get_showblock_item")) {
                ItemProperties.register(
                        BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, s)),
                        ResourceLocation.parse("direction"),
                        (itemStack, clientWorld, livingEntity, i) -> itemStack.getOrDefault(DataComponentRegistry.TRANS_DIRECTION.get(), 0) * 0.1F
                );
            }

            ItemProperties.register(
                    ItemRegistry.GET_BLOCKSTATE_ITEM.get(),
                    ResourceLocation.parse("direction"),
                    (itemStack, clientWorld, livingEntity, i) -> {
                        BlockState blockState = itemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE, Blocks.AIR.defaultBlockState());
                        if (!blockState.equals(Blocks.AIR.defaultBlockState())) return 1;
                        return 0;
                    }
            );
        });
    }
}
