package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.yuushya.modelling.utils.YuushyaUtils.BLOCK_ENTITY_TAG;

public class TransformDataListNetwork {
    public static final ResourceLocation TRANSFORM_DATA_LIST_PACKET_ID = new ResourceLocation(Yuushya.MOD_ID_USED, "transform_data_list_packet");

    private static final Map<String,ItemStack> HandlingCache = new HashMap<>();

    public record TransformDataListPacket(
            CompoundTag tag
    )  {
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
                    if(!tag.contains("Blocks") && HandlingCache.containsKey(hash)){
                        menu.setupResultSlotServer(HandlingCache.get(hash));
                    }
                    else{
                        ItemStack itemStack = YuushyaRegistries.ITEMS.get("showblock").get().getDefaultInstance();
                        itemStack.addTagElement(BLOCK_ENTITY_TAG,tag);
                        itemStack.setHoverName(new TextComponent(name));
                        HandlingCache.put(hash,itemStack);
                        menu.setupResultSlotServer(itemStack);
                    }
                }
            });
        }

    }

    public static final Set<String> SendingCache = new HashSet<>();
    public static void updateSendingCache(String name){
        SendingCache.remove(name);
    }

    //architectury提供的另一种风格的api
    public static void sendToServerSide(EngraveItemResult itemResult){
        String name = itemResult.getName();
        CompoundTag tag;
        if(SendingCache.contains(name)){
            tag = new CompoundTag();
        }
        else{
            ItemStack itemStack = itemResult.getResultItem();
            tag = itemStack.getOrCreateTagElement(BLOCK_ENTITY_TAG);
        }
        tag.putString("ItemName",name);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new TransformDataListPacket(tag).encoder(buf);
        NetworkManager.sendToServer(TRANSFORM_DATA_LIST_PACKET_ID,buf);
    }

    public static void registerServerSideReceiver(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,TRANSFORM_DATA_LIST_PACKET_ID, (buf, context) -> {
            TransformDataListPacket packet = TransformDataListPacket.decoder(buf);
            packet.handler(()->context);
        });
    }

}
