package com.yuushya.modelling.network;

import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.YuushyaDataTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PickColorPacket {
    private final int color;

    public PickColorPacket(int color) {
        this.color = color;
    }

    public PickColorPacket(FriendlyByteBuf buffer) {
        this.color = buffer.readVarInt();
    }

    public static void handle(PickColorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            int color = packet.color;
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ItemStack mainHandItem = sender.getMainHandItem();
                if (mainHandItem.is(ItemRegistry.COLOR_PICKER_ITEM.get())) {
                    YuushyaDataTags.setColorData(mainHandItem, color);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(int color) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new PickColorPacket(color));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.color);
    }
}
