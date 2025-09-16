package com.yuushya.modelling;

import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.TransformDataListPacket;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkManager;

public class Yuushya {
    public static final String MOD_ID = "yuushya";
    public static final String MOD_ID_USED = "yuushya_modelling";
    public static void init(){
        YuushyaRegistries.registerAll();
        registerNetwork();
    }

    private static void registerNetwork(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TransformDataOncePacket.TYPE, TransformDataOncePacket.STREAM_CODEC, TransformDataOncePacket::handler);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ItemTransformDataOncePacket.TYPE, ItemTransformDataOncePacket.STREAM_CODEC, ItemTransformDataOncePacket::handler);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TransformDataListPacket.TYPE, TransformDataListPacket.STREAM_CODEC, TransformDataListPacket::handler);
    }
}
