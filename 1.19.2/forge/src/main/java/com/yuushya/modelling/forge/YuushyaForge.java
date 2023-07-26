package com.yuushya.modelling.forge;

import com.yuushya.modelling.forge.item.GetBlockStateItemForge;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.yuushya.modelling.Yuushya.MOD_ID_USED;
import static com.yuushya.modelling.registries.YuushyaRegistries.YUUSHYA_MODELLING;

@Mod(MOD_ID_USED)
public class YuushyaForge {
    public YuushyaForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Yuushya.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItemForge(new Item.Properties().tab(YUUSHYA_MODELLING), 3));
        Yuushya.init();
    }
}
