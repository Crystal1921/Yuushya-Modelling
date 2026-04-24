package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@SuppressWarnings("removal")
public class YuushyaModellingNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Yuushya.MOD_ID_USED, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(TransformDataOncePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TransformDataOncePacket::encode)
                .decoder(TransformDataOncePacket::new)
                .consumerMainThread(TransformDataOncePacket::handle)
                .add();

        INSTANCE.messageBuilder(ItemTransformDataOncePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ItemTransformDataOncePacket::encode)
                .decoder(ItemTransformDataOncePacket::new)
                .consumerMainThread(ItemTransformDataOncePacket::handle)
                .add();

        INSTANCE.messageBuilder(TextTransformDataOncePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TextTransformDataOncePacket::encode)
                .decoder(TextTransformDataOncePacket::new)
                .consumerMainThread(TextTransformDataOncePacket::handle)
                .add();

        INSTANCE.messageBuilder(TextLinesPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TextLinesPacket::encode)
                .decoder(TextLinesPacket::new)
                .consumerMainThread(TextLinesPacket::handle)
                .add();

        INSTANCE.messageBuilder(TransformDataListPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TransformDataListPacket::encode)
                .decoder(TransformDataListPacket::new)
                .consumerMainThread(TransformDataListPacket::handle)
                .add();

        INSTANCE.messageBuilder(ItemStackPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ItemStackPacket::encode)
                .decoder(ItemStackPacket::new)
                .consumerMainThread(ItemStackPacket::handle)
                .add();

        INSTANCE.messageBuilder(ReloadModelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReloadModelPacket::encode)
                .decoder(ReloadModelPacket::new)
                .consumerMainThread(ReloadModelPacket::handle)
                .add();

        INSTANCE.messageBuilder(UpdateAOPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateAOPacket::encode)
                .decoder(UpdateAOPacket::new)
                .consumerMainThread(UpdateAOPacket::handle)
                .add();

        INSTANCE.messageBuilder(PickColorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickColorPacket::encode)
                .decoder(PickColorPacket::new)
                .consumerMainThread(PickColorPacket::handle)
                .add();
    }
}
