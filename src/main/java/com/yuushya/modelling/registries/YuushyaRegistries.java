package com.yuushya.modelling.registries;

import net.minecraftforge.eventbus.api.IEventBus;

public class YuushyaRegistries {
    // Delegate to the new registry classes
    public static void register(IEventBus bus) {
        BlockRegistry.register(bus);
        ItemRegistry.register(bus);
        BlockEntityRegistry.register(bus);
        DataComponentRegistry.register(bus);
        MenuRegistry.register(bus);
        GroupRegistry.register(bus);
    }
}