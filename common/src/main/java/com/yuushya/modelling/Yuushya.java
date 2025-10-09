package com.yuushya.modelling;

import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.TransformDataListPacket;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Yuushya {
    public static final String MOD_ID = "yuushya";
    public static final String MOD_ID_USED = "yuushya_modelling";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init() {
        YuushyaRegistries.registerAll();
        registerNetwork();
    }

    private static void registerNetwork() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TransformDataOncePacket.TYPE, TransformDataOncePacket.STREAM_CODEC, TransformDataOncePacket::handler);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ItemTransformDataOncePacket.TYPE, ItemTransformDataOncePacket.STREAM_CODEC, ItemTransformDataOncePacket::handler);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TransformDataListPacket.TYPE, TransformDataListPacket.STREAM_CODEC, TransformDataListPacket::handler);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ItemStackPacket.TYPE, ItemStackPacket.STREAM_CODEC, ItemStackPacket::handler);
    }
}
