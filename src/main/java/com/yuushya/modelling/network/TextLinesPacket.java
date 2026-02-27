package com.yuushya.modelling.network;

import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TextLinesPacket {
    private final BlockPos blockPos;
    private final int slot;
    private final List<String> textLines;

    public TextLinesPacket(BlockPos blockPos, int slot, List<String> textLines) {
        this.blockPos = blockPos;
        this.slot = slot;
        this.textLines = textLines;
    }

    public TextLinesPacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readBlockPos();
        this.slot = buffer.readVarInt();
        this.textLines = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.blockPos);
        buffer.writeVarInt(this.slot);
        buffer.writeCollection(this.textLines, FriendlyByteBuf::writeUtf);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, List<String> textLines) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new TextLinesPacket(blockPos, slot, textLines));
    }

    public static void handle(TextLinesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ctx.get().getSender().level();
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
        ctx.get().setPacketHandled(true);
    }
}
