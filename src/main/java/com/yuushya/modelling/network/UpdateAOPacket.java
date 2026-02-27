package com.yuushya.modelling.network;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.FULL_BLOCK;

public class UpdateAOPacket {
    private final boolean enableAO;
    private final boolean fullBlock;
    private final BlockPos blockPos;

    public UpdateAOPacket(boolean enableAO, boolean fullBlock, BlockPos blockPos) {
        this.enableAO = enableAO;
        this.fullBlock = fullBlock;
        this.blockPos = blockPos;
    }

    public UpdateAOPacket(FriendlyByteBuf buffer) {
        this.enableAO = buffer.readBoolean();
        this.fullBlock = buffer.readBoolean();
        this.blockPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enableAO);
        buffer.writeBoolean(this.fullBlock);
        buffer.writeBlockPos(this.blockPos);
    }

    public static void handle(UpdateAOPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ctx.get().getSender().level();
            if (level instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                if (blockEntity instanceof ItemBlockEntity) {
                    level.setBlockAndUpdate(blockEntity.getBlockPos(), blockEntity.getBlockState().setValue(ENABLE_AO, packet.enableAO).setValue(FULL_BLOCK, packet.fullBlock));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(boolean enableAO, boolean fullBlock, BlockPos blockPos) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new UpdateAOPacket(enableAO, fullBlock, blockPos));
    }
}
