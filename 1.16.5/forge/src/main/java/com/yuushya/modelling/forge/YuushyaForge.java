package com.yuushya.modelling.forge;

import com.yuushya.modelling.forge.item.GetBlockStateItemForge;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.yuushya.modelling.registries.YuushyaRegistries.YUUSHYA_MODELLING;

@Mod("yuushya_modelling")
public class YuushyaForge {
    public YuushyaForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Yuushya.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties().tab(YUUSHYA_MODELLING).setISTER(GetBlockStateItemForge::getRenderCall), 3));
        Yuushya.init();
    }
}
