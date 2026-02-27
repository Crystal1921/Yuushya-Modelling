package com.yuushya.modelling.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class YuushyaModellingNetwork {
    public static final String VERSION = "1.0";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(TransformDataOncePacket.TYPE, TransformDataOncePacket.STREAM_CODEC, TransformDataOncePacket::handler);
        registrar.playToServer(ItemTransformDataOncePacket.TYPE, ItemTransformDataOncePacket.STREAM_CODEC, ItemTransformDataOncePacket::handler);
        registrar.playToServer(TextTransformDataOncePacket.TYPE, TextTransformDataOncePacket.STREAM_CODEC, TextTransformDataOncePacket::handler);
        registrar.playToServer(TextLinesPacket.TYPE, TextLinesPacket.STREAM_CODEC, TextLinesPacket::handler);
        registrar.playToServer(TransformDataListPacket.TYPE, TransformDataListPacket.STREAM_CODEC, TransformDataListPacket::handler);
        registrar.playToServer(ItemStackPacket.TYPE, ItemStackPacket.STREAM_CODEC, ItemStackPacket::handler);
        registrar.playToClient(ReloadModelPacket.TYPE, ReloadModelPacket.STREAM_CODEC, ReloadModelPacket::handler);
        registrar.playToServer(UpdateAOPacket.TYPE, UpdateAOPacket.STREAM_CODEC, UpdateAOPacket::handler);
    }
}
