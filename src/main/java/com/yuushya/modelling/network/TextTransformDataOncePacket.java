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

import java.util.function.Supplier;

public class TextTransformDataOncePacket {
    private final BlockPos blockPos;
    private final TextTransformType transformType;
    private final int slot;
    private final double number;

    public TextTransformDataOncePacket(BlockPos blockPos, TextTransformType transformType, int slot, double number) {
        this.blockPos = blockPos;
        this.transformType = transformType;
        this.slot = slot;
        this.number = number;
    }

    public TextTransformDataOncePacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readBlockPos();
        this.transformType = buffer.readEnum(TextTransformType.class);
        this.slot = buffer.readVarInt();
        this.number = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.blockPos);
        buffer.writeEnum(this.transformType);
        buffer.writeVarInt(this.slot);
        buffer.writeDouble(this.number);
    }

    public static void sendToServerSideSuccess(BlockPos blockPos) {
        sendToServerSide(blockPos, 0, TextTransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, TextTransformType type, double number) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new TextTransformDataOncePacket(blockPos, type, slot, number));
    }

    public static void handle(TextTransformDataOncePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ctx.get().getSender().level();
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
        ctx.get().setPacketHandled(true);
    }
}
