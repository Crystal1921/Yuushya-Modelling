package com.yuushya.modelling.network;

import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TransformDataOncePacket {
    private final BlockPos blockPos;
    private final TransformType transformType;
    private final int slot;
    private final double number;

    public TransformDataOncePacket(BlockPos blockPos, TransformType transformType, int slot, double number) {
        this.blockPos = blockPos;
        this.transformType = transformType;
        this.slot = slot;
        this.number = number;
    }

    public TransformDataOncePacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readBlockPos();
        this.transformType = buffer.readEnum(TransformType.class);
        this.slot = buffer.readVarInt();
        this.number = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.blockPos);
        buffer.writeEnum(this.transformType);
        buffer.writeVarInt(this.slot);
        buffer.writeDouble(this.number);
    }

    public static void handle(TransformDataOncePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ctx.get().getSender().level();
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
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServerSideSuccess(BlockPos blockPos) {
        sendToServerSide(blockPos, 0, TransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, TransformType type, double number) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new TransformDataOncePacket(blockPos, type, slot, number));
    }
}
