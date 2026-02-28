package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.AbstractEngraveMenu;
import com.yuushya.modelling.gui.engrave.IEngraveResult;
import com.yuushya.modelling.utils.YuushyaDataTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TransformDataListPacket {
    private final CompoundTag tag;

    public static final Set<String> SendingCache = new HashSet<>();
    private static final Map<String, ItemStack> HandlingCache = new HashMap<>();

    public TransformDataListPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public TransformDataListPacket(FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(this.tag);
    }

    public static void updateSendingCache(String name) {
        SendingCache.remove(name);
    }

    public static void sendToServerSide(IEngraveResult itemResult) {
        String name = itemResult.getName();
        CompoundTag tag;
        if (SendingCache.contains(name)) {
            tag = new CompoundTag();
        } else {
            ItemStack itemStack = itemResult.getResultItem();
            CompoundTag transformDataTag = YuushyaDataTags.getTransformData(itemStack);
            CompoundTag textDataTag = YuushyaDataTags.getTextData(itemStack);
            // Use non-empty tag, prefer transform data for block/item blocks
            tag = !transformDataTag.isEmpty() ? transformDataTag :
                  !textDataTag.isEmpty() ? textDataTag : new CompoundTag();
        }
        tag.putString("ItemName", name);
        YuushyaModellingNetwork.INSTANCE.sendToServer(new TransformDataListPacket(tag));
    }

    public static void handle(TransformDataListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            AbstractContainerMenu abstractContainerMenu = player.containerMenu;
            if (abstractContainerMenu instanceof AbstractEngraveMenu menu) {
                if (!menu.stillValid(player)) {
                    return;
                }
                String name = packet.tag.getString("ItemName");
                String hash = player.getStringUUID() + name;
                if (!packet.tag.contains("Blocks") && HandlingCache.containsKey(hash)) {
                    menu.setupResultSlotServer(HandlingCache.get(hash));
                } else {
                    // Determine which item type to create based on the menu's recipe type
                    String itemType = switch (menu.getBlockType()) {
                        case BLOCK -> "itemblock";
                        case ITEM -> "showblock";
                        case TEXT -> "textblock";
                    };
                    ItemStack itemStack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, itemType)).getDefaultInstance();
                    itemStack.setHoverName(Component.literal(name));
                    // Store data based on item type
                    if (itemType.equals("textblock")) {
                        YuushyaDataTags.setTextData(itemStack, packet.tag);
                    } else {
                        YuushyaDataTags.setTransformData(itemStack, packet.tag);
                    }
                    HandlingCache.put(hash, itemStack);
                    menu.setupResultSlotServer(itemStack);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
