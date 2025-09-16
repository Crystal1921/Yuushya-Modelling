package com.yuushya.modelling.network;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformType;
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

public record TransformDataOncePacket(
        BlockPos blockPos,
        TransformType transformType,
        int slot,
        double number
) implements CustomPacketPayload {
    public static final ResourceLocation TRANSFORM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "transform_data_packet");
    public static final Type<TransformDataOncePacket> TYPE = new Type<>(TRANSFORM_DATA_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, TransformDataOncePacket> STREAM_CODEC = CustomPacketPayload.codec(TransformDataOncePacket::encoder, TransformDataOncePacket::decoder);
    private static final Logger LOGGER = LogUtils.getLogger();

    //buf -> pack
    public static TransformDataOncePacket decoder(FriendlyByteBuf buf) {
        return new TransformDataOncePacket(
                buf.readBlockPos(),
                TransformType.from(buf.readByte()),
                buf.readByte(),
                buf.readDouble()
        );
    }

    //after receive
    public static void handler(TransformDataOncePacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            Level level = ctx.getPlayer().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockState(packet.blockPos).getBlock() instanceof ShowBlock) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                    if (!(blockEntity instanceof ShowBlockEntity showBlockEntity)) {
                        return;
                    }
                    packet.transformType.modify(showBlockEntity, packet.slot, packet.number);
                }
            }
        });
    }

    public static void sendToServerSideSuccess(BlockPos blockPos) {
        sendToServerSide(blockPos, 0, TransformType.SUCCESS, 0);
    }

    //architectury提供的另一种风格的api
    public static void sendToServerSide(BlockPos blockPos, int slot, TransformType type, double number) {
        NetworkManager.sendToServer(new TransformDataOncePacket(blockPos, type, slot, number));
    }

    //pack -> buf
    public void encoder(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeByte(transformType.type);
        buf.writeByte(slot);
        buf.writeDouble(number);
    }

    @Override
    public @NotNull Type<TransformDataOncePacket> type() {
        return TYPE;
    }

}
