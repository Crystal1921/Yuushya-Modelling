package com.yuushya.modelling.network;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public record ItemTransformDataOncePacket(
        BlockPos blockPos,
        ItemTransformType transformType,
        int slot,
        double number
) implements CustomPacketPayload {
    public static final ResourceLocation ITEM_TRANSFORM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "item_transform_data_packet");
    public static final Type<ItemTransformDataOncePacket> TYPE = new Type<>(ITEM_TRANSFORM_DATA_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, ItemTransformDataOncePacket> STREAM_CODEC = CustomPacketPayload.codec(ItemTransformDataOncePacket::encoder, ItemTransformDataOncePacket::decoder);
    private static final Logger LOGGER = LogUtils.getLogger();

    //buf -> pack
    public static ItemTransformDataOncePacket decoder(FriendlyByteBuf buf) {
        return new ItemTransformDataOncePacket(
                buf.readBlockPos(),
                ItemTransformType.from(buf.readByte()),
                buf.readByte(),
                buf.readDouble()
        );
    }

    public static void sendToServerSideSuccess(BlockPos blockPos){
        sendToServerSide(blockPos, 0, ItemTransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, ItemTransformType type, double number){
        NetworkManager.sendToServer(new ItemTransformDataOncePacket(blockPos, type, slot, number));
    }

    //pack -> buf
    public void encoder(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeByte(transformType.type);
        buf.writeByte(slot);
        buf.writeDouble(number);
    }

    //after receive
    public static void handler(ItemTransformDataOncePacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            Level level = ctx.getPlayer().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockState(packet.blockPos).getBlock() instanceof ItemBlock) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                    if (!(blockEntity instanceof ItemBlockEntity itemBlockEntity)) {
                        return;
                    }
                    packet.transformType.modify(itemBlockEntity, packet.slot, packet.number);
                }
            }
        });
    }

    @Override
    public @NotNull Type<ItemTransformDataOncePacket> type() {
        return TYPE;
    }
}
