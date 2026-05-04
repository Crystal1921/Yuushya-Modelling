package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.command.ReloadModelCommand;
import com.yuushya.modelling.event.RegistryEvent;
import com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.engrave.EngraveTextResultLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ReloadModelPacket(ReloadModelCommand.ReloadType reloadType) implements CustomPacketPayload {
    public static final Identifier RELOAD_MODEL_PACKET_ID = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "reload_model_packet");
    public static final Type<ReloadModelPacket> TYPE = new Type<>(RELOAD_MODEL_PACKET_ID);

    public static final StreamCodec<FriendlyByteBuf, ReloadModelPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeCodecs.enumCodec(ReloadModelCommand.ReloadType.class),
            ReloadModelPacket::reloadType,
            ReloadModelPacket::new
    );

    public static void handler(ReloadModelPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level instanceof ClientLevel client) {
                switch (packet.reloadType) {
                    case BLOCKS -> {
                        EngraveBlockResultLoader.SHOWBLOCK_ITEM_MAP.clear();
                        EngraveBlockResultLoader.load(client.registryAccess());
                    }
                    case ITEMS -> {
                        EngraveItemResultLoader.ITEMBLOCK_ITEM_MAP.clear();
                        EngraveItemResultLoader.load(client.registryAccess());
                    }
                    case TEXTS -> {
                        EngraveTextResultLoader.TEXTBLOCK_ITEM_MAP.clear();
                        EngraveTextResultLoader.load(client.registryAccess());
                    }
                    case ALL -> RegistryEvent.load(client.registryAccess());
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
