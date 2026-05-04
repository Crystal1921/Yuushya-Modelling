package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PickColorPacket(int color) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, PickColorPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PickColorPacket::color,
            PickColorPacket::new
    );

    public static final Type<PickColorPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "pick_color_packet"));

    public static void handler(PickColorPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.is(ItemRegistry.COLOR_PICKER_ITEM)) {
                mainHandItem.set(DataComponentRegistry.COLOR_DATA, packet.color());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
