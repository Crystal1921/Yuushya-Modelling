package com.yuushya.modelling.fabric;

import com.yuushya.modelling.fabriclike.YuushyaClientFabricLike;
import net.fabricmc.api.ClientModInitializer;

public class YuushyaClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        YuushyaClientFabricLike.onInitializeClient();
    }
}