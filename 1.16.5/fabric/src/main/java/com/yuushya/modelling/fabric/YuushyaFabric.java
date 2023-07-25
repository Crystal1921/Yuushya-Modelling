package com.yuushya.modelling.fabric;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Item;

import static com.yuushya.modelling.registries.YuushyaRegistries.YUUSHYA_MODELLING;

public class YuushyaFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties().tab(YUUSHYA_MODELLING), 3));
        Yuushya.init();
        //ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> CollisionFileReader.readAllFileSelf());
    }
}
