package com.yuushya.modelling;

import com.yuushya.modelling.network.YuushyaModellingNetwork;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.yuushya.modelling.Yuushya.MOD_ID_USED;

@Mod(MOD_ID_USED)
@SuppressWarnings("removal")
public class YuushyaNeoForge {
    public YuushyaNeoForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        YuushyaRegistries.register(eventBus);
        YuushyaModellingNetwork.register();
    }
}


