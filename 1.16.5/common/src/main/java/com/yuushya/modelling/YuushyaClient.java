package com.yuushya.modelling;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntityRender;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveMenu;
import com.yuushya.modelling.gui.engrave.EngraveScreen;
import com.yuushya.modelling.registries.YuushyaRegistries;
import me.shedaniel.architectury.registry.BlockEntityRenderers;
import me.shedaniel.architectury.registry.ItemPropertiesRegistry;
import me.shedaniel.architectury.registry.MenuRegistry;
import me.shedaniel.architectury.registry.RenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;
import java.util.List;

public class YuushyaClient {
    public static void onInitializeClient(){
        RenderTypes.register(RenderType.cutout(), YuushyaRegistries.SHOW_BLOCK.get());
        BlockEntityRenderers.registerRenderer((BlockEntityType<ShowBlockEntity>) YuushyaRegistries.SHOW_BLOCK_ENTITY.get(), ShowBlockEntityRender::new);

        for (String s: Arrays.asList("rot_trans_item","pos_trans_item","micro_pos_trans_item","get_showblock_item"))
            ItemPropertiesRegistry.register(YuushyaRegistries.ITEMS.get(s).get(),new ResourceLocation("direction"),(itemStack, clientWorld, livingEntity) -> itemStack.hasTag() ? itemStack.getTag().getFloat("TransDirection")*0.1F : 0);
        ItemPropertiesRegistry.register(YuushyaRegistries.ITEMS.get("get_blockstate_item").get(),new ResourceLocation("direction"),(itemStack, clientWorld, livingEntity) -> itemStack.hasTag() ? 1 : 0);

        EngraveItemResultLoader.load();
        MenuRegistry.registerScreenFactory((MenuType<EngraveMenu>) YuushyaRegistries.ENGRAVE_MENU.get(), EngraveScreen::new);
    }

}
