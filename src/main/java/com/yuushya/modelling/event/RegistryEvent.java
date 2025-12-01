package com.yuushya.modelling.event;

import com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import net.minecraft.core.RegistryAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class RegistryEvent {
    @SubscribeEvent
    public static void afterRegistry(DataMapsUpdatedEvent event) {
        RegistryAccess registries = event.getRegistries();
        RegistryEvent.load(registries);
    }

    public static void load(RegistryAccess registryAccess) {
        EngraveBlockResultLoader.SHOWBLOCK_ITEM_MAP.clear();
        EngraveItemResultLoader.ITEMBLOCK_ITEM_MAP.clear();
        EngraveBlockResultLoader.load(registryAccess);
        EngraveItemResultLoader.load(registryAccess);
    }
}
