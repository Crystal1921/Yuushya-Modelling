package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.TransformType;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TransformDataListNetwork {
    public static final ResourceLocation TRANSFORM_DATA_LIST_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "transform_data_list_packet");
    public static final StreamCodec<FriendlyByteBuf, TransformDataListPacket> STREAM_CODEC = CustomPacketPayload.codec(TransformDataListPacket::encoder, TransformDataListPacket::decoder);
    public static final CustomPacketPayload.Type<TransformDataListPacket> TRANSFORM_DATA_LIST_PACKET_TYPE = new CustomPacketPayload.Type<>(TRANSFORM_DATA_LIST_PACKET_ID);

    private static final Map<String,ItemStack> HandlingCache = new HashMap<>();

    public record TransformDataListPacket(
            CompoundTag tag
    )   implements CustomPacketPayload {
        //buf -> pack
        public static TransformDataListPacket decoder(FriendlyByteBuf buf){
            return new TransformDataListPacket(
                    buf.readNbt()
            );
        }

        //pack -> buf
        public void encoder(FriendlyByteBuf buf){
            buf.writeNbt(tag);
        }

        //after receive
        public void handler(Supplier<NetworkManager.PacketContext> ctx){
            ctx.get().queue(() -> {
                Player player = ctx.get().getPlayer();
                AbstractContainerMenu abstractContainerMenu = player.containerMenu;
                if (abstractContainerMenu instanceof EngraveMenu menu) {
                    if (!menu.stillValid(player)) {
                        return;
                    }
                    String name = tag.getString("ItemName");
                    String hash = player.getStringUUID()+name;
                    if(HandlingCache.containsKey(hash)){
                        menu.setupResultSlotServer(HandlingCache.get(hash));
                    }
                    else{
                        ItemStack itemStack = YuushyaRegistries.ITEMS.get("showblock").get().getDefaultInstance();
                        itemStack.set(DataComponents.ITEM_NAME, Component.literal(name));
                        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                        HandlingCache.put(hash,itemStack);
                        menu.setupResultSlotServer(itemStack);
                    }
                }
            });
        }

        @Override
        public CustomPacketPayload.Type<TransformDataListPacket> type() {
            return TRANSFORM_DATA_LIST_PACKET_TYPE;
        }

    }

    public static final Set<String> SendingCache = new HashSet<>();

    //architectury提供的另一种风格的api
    public static void sendToServerSide(EngraveItemResult itemResult){
        String name = itemResult.getName();
        if(SendingCache.contains(name)){
            CompoundTag tag = new CompoundTag();
            tag.putString("ItemName",name);
            NetworkManager.sendToServer(new TransformDataListPacket(tag));
        }
        else{
            ItemStack itemStack = itemResult.getResultItem();
            CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY);
            CompoundTag tag = data.copyTag();
            tag.putString("ItemName",name);
//        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//        new TransformDataListPacket(tag).encoder(buf);
            NetworkManager.sendToServer(new TransformDataListPacket(tag));
        }
    }

    public static void registerServerSideReceiver(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRANSFORM_DATA_LIST_PACKET_TYPE,STREAM_CODEC, (packet, context) -> {
            packet.handler(()->context);
        });
    }

}
