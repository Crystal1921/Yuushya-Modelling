package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TextTransformDataOncePacket(
        BlockPos blockPos,
        TextTransformType transformType,
        int slot,
        double number
) implements CustomPacketPayload {
    public static final Identifier TEXT_TRANSFORM_DATA_PACKET_ID = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "text_transform_data_packet");
    public static final Type<TextTransformDataOncePacket> TYPE = new Type<>(TEXT_TRANSFORM_DATA_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, TextTransformDataOncePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TextTransformDataOncePacket::blockPos,
            NeoForgeCodecs.enumCodec(TextTransformType.class),
            TextTransformDataOncePacket::transformType,
            ByteBufCodecs.VAR_INT,
            TextTransformDataOncePacket::slot,
            ByteBufCodecs.DOUBLE,
            TextTransformDataOncePacket::number,
            TextTransformDataOncePacket::new
    );

    public static void sendToServerSideSuccess(BlockPos blockPos) {
        sendToServerSide(blockPos, 0, TextTransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, TextTransformType type, double number) {
        ClientPacketDistributor.sendToServer(new TextTransformDataOncePacket(blockPos, type, slot, number));
    }

    //after receive
    public static void handler(TextTransformDataOncePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockState(packet.blockPos).getBlock() instanceof TextBlock) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                    if (!(blockEntity instanceof TextBlockEntity textBlockEntity)) {
                        return;
                    }
                    packet.transformType.modify(textBlockEntity, packet.slot, packet.number);
                }
            }
        });
    }

    @Override
    public @NotNull Type<TextTransformDataOncePacket> type() {
        return TYPE;
    }
}
