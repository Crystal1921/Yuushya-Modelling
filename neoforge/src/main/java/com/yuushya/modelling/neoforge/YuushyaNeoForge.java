package com.yuushya.modelling.neoforge;

import com.yuushya.modelling.neoforge.item.GetBlockStateItemForge;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.yuushya.modelling.Yuushya.MOD_ID;
import static com.yuushya.modelling.Yuushya.MOD_ID_USED;

@Mod(MOD_ID_USED)
public class YuushyaNeoForge {
    //public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("yuushya");
    //public static final DeferredItem<Item> TEST = ITEMS.register("test",()->new Item(new Item. Properties()));
    public YuushyaNeoForge(IEventBus modbus) {
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItemForge(new Item.Properties(), 3));
        Yuushya.init();
        //ITEMS.register(modbus);
    }
}


