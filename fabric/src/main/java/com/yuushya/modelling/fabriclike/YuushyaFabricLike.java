package com.yuushya.modelling.fabriclike;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.world.item.Item;

import static com.yuushya.modelling.registries.YuushyaRegistries.YUUSHYA_MODELLING;

public class YuushyaFabricLike {
    public static void init(){
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties(), 3));
        Yuushya.init();
        //ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> CollisionFileReader.readAllFileSelf());
    }
}
