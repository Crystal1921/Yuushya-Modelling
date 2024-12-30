package com.yuushya.modelling.fabric;

import com.yuushya.modelling.fabriclike.YuushyaFabricLike;
import net.fabricmc.api.ModInitializer;

public class YuushyaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YuushyaFabricLike.init();
    }
}
