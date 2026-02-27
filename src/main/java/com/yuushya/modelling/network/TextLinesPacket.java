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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TextLinesPacket(
        BlockPos blockPos,
        int slot,
        List<String> textLines
) implements CustomPacketPayload {
    public static final ResourceLocation TEXT_LINES_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "text_lines_packet");
    public static final Type<TextLinesPacket> TYPE = new Type<>(TEXT_LINES_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, TextLinesPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TextLinesPacket::blockPos,
            ByteBufCodecs.VAR_INT,
            TextLinesPacket::slot,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            TextLinesPacket::textLines,
            TextLinesPacket::new
    );

    public static void sendToServerSide(BlockPos blockPos, int slot, List<String> textLines) {
        PacketDistributor.sendToServer(new TextLinesPacket(blockPos, slot, textLines));
    }

    //after receive
    public static void handler(TextLinesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockState(packet.blockPos).getBlock() instanceof TextBlock) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                    if (!(blockEntity instanceof TextBlockEntity textBlockEntity)) {
                        return;
                    }
                    TextTransformType.TEXT_LINES.modify(textBlockEntity, packet.slot, packet.textLines);
                }
            }
        });
    }

    @Override
    public @NotNull Type<TextLinesPacket> type() {
        return TYPE;
    }
}
