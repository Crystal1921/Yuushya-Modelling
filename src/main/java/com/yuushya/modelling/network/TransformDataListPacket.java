package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.AbstractEngraveMenu;
import com.yuushya.modelling.gui.engrave.IEngraveResult;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record TransformDataListPacket(
        String name, TypedEntityData<BlockEntityType<?>> typeData
) implements CustomPacketPayload {
    public static final Identifier TRANSFORM_DATA_LIST_PACKET_ID = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "transform_data_list_packet");
    public static final Type<TransformDataListPacket> TYPE = new Type<>(TRANSFORM_DATA_LIST_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TransformDataListPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TransformDataListPacket::name,
            TypedEntityData.streamCodec(ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE)),
            TransformDataListPacket::typeData,
            TransformDataListPacket::new
    );

    public static void sendToServerSide(IEngraveResult itemResult) {
        String name = itemResult.getName();
        ItemStack resultItem = itemResult.getResultItem();
        TypedEntityData<BlockEntityType<?>> typedEntityData = resultItem.get(DataComponents.BLOCK_ENTITY_DATA);
        ClientPacketDistributor.sendToServer(new TransformDataListPacket(name, typedEntityData));
    }

    //after receive
    public static void handler(TransformDataListPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            AbstractContainerMenu abstractContainerMenu = player.containerMenu;
            if (abstractContainerMenu instanceof AbstractEngraveMenu menu) {
                if (!menu.stillValid(player)) {
                    return;
                }

                // Determine which item type to create based on the menu's recipe type
                String itemType = switch (menu.getBlockType()) {
                    case BLOCK -> "itemblock";
                    case ITEM -> "showblock";
                    case TEXT -> "textblock";
                };
                Optional<Holder.Reference<Item>> itemReference = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, itemType));
                if (itemReference.isPresent()) {
                    ItemStack itemStack = itemReference.get().value().getDefaultInstance();
                    itemStack.set(DataComponents.ITEM_NAME, Component.literal(packet.name));
                    itemStack.set(DataComponents.BLOCK_ENTITY_DATA, packet.typeData);
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
