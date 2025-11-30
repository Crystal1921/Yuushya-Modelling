package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.ITEM_STACK;

public record ItemStackPacket(BlockPos blockPos, int slot, ItemStack itemStack) implements CustomPacketPayload {
    public static final ResourceLocation ITEM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "item_data_packet");
    public static final Type<ItemStackPacket> TYPE = new Type<>(ITEM_DATA_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ItemStackPacket::blockPos,
            ByteBufCodecs.VAR_INT,
            ItemStackPacket::slot,
            ItemStack.STREAM_CODEC,
            ItemStackPacket::itemStack,
            ItemStackPacket::new
    );

    public static void handler(ItemStackPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockEntity(packet.blockPos) instanceof ItemBlockEntity itemBlockEntity) {
                    ITEM_STACK.modify(itemBlockEntity, packet.slot, packet.itemStack);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
