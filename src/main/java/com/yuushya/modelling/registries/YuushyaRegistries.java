package com.yuushya.modelling.registries;

import net.minecraftforge.eventbus.api.IEventBus;

public class YuushyaRegistries {
    // Delegate to the new registry classes
    public static void register(IEventBus bus) {
        BlockRegistry.register(bus);
        ItemRegistry.register(bus);
        BlockEntityRegistry.register(bus);
        // DataComponentRegistry removed - using CompoundTag instead in 1.20
        MenuRegistry.register(bus);
        GroupRegistry.register(bus);
    }
}