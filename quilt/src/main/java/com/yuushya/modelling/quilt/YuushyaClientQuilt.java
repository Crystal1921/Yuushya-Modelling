package com.yuushya.modelling.quilt;

import com.yuushya.modelling.fabriclike.YuushyaClientFabricLike;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class YuushyaClientQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        YuushyaClientFabricLike.onInitializeClient();
    }
}