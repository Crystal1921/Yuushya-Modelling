package com.yuushya.modelling;

import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import static com.yuushya.modelling.Yuushya.MOD_ID_USED;

@Mod(MOD_ID_USED)
public class YuushyaNeoForge {
    public YuushyaNeoForge(IEventBus modbus) {
        YuushyaRegistries.register(modbus);
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Yuushya.MOD_ID_USED, name);
    }
}


