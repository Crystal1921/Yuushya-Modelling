package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.engrave.EngraveBlockResult;
import com.yuushya.modelling.gui.engrave.EngraveMenu;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record TransformDataListPacket(
        CompoundTag tag
) implements CustomPacketPayload {
    public static final ResourceLocation TRANSFORM_DATA_LIST_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "transform_data_list_packet");
    public static final Type<TransformDataListPacket> TYPE = new Type<>(TRANSFORM_DATA_LIST_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, TransformDataListPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            TransformDataListPacket::tag,
            TransformDataListPacket::new
    );
    public static final Set<String> SendingCache = new HashSet<>();
    private static final Map<String, ItemStack> HandlingCache = new HashMap<>();

    public static void updateSendingCache(String name) {
        SendingCache.remove(name);
    }

    //architectury提供的另一种风格的api
    public static void sendToServerSide(EngraveBlockResult itemResult) {
        String name = itemResult.getName();
        CompoundTag tag;
        if (SendingCache.contains(name)) {
            tag = new CompoundTag();
        } else {
            ItemStack itemStack = itemResult.getResultItem();
            CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            tag = data.copyTag();
        }
        tag.putString("ItemName", name);
        NetworkManager.sendToServer(new TransformDataListPacket(tag));
    }

    //after receive
    public static void handler(TransformDataListPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            Player player = ctx.getPlayer();
            AbstractContainerMenu abstractContainerMenu = player.containerMenu;
            if (abstractContainerMenu instanceof EngraveMenu menu) {
                if (!menu.stillValid(player)) {
                    return;
                }
                String name = packet.tag.getString("ItemName");
                String hash = player.getStringUUID() + name;
                if (!packet.tag.contains("Blocks") && HandlingCache.containsKey(hash)) {
                    menu.setupResultSlotServer(HandlingCache.get(hash));
                } else {
                    ItemStack itemStack = YuushyaRegistries.ITEMS.get("showblock").get().getDefaultInstance();
                    itemStack.set(DataComponents.ITEM_NAME, Component.literal(name));
                    itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(packet.tag));
                    HandlingCache.put(hash, itemStack);
                    menu.setupResultSlotServer(itemStack);
                }
            }
        });
    }

    @Override
    public @NotNull Type<TransformDataListPacket> type() {
        return TYPE;
    }

}
