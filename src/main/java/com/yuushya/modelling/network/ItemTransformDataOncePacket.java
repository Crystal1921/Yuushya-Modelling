package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
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

public record ItemTransformDataOncePacket(
        BlockPos blockPos,
        ItemTransformType transformType,
        int slot,
        double number
) implements CustomPacketPayload {
    public static final ResourceLocation ITEM_TRANSFORM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "item_transform_data_packet");
    public static final Type<ItemTransformDataOncePacket> TYPE = new Type<>(ITEM_TRANSFORM_DATA_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, ItemTransformDataOncePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ItemTransformDataOncePacket::blockPos,
            NeoForgeCodecs.enumCodec(ItemTransformType.class),
            ItemTransformDataOncePacket::transformType,
            ByteBufCodecs.VAR_INT,
            ItemTransformDataOncePacket::slot,
            ByteBufCodecs.DOUBLE,
            ItemTransformDataOncePacket::number,
            ItemTransformDataOncePacket::new
    );

    public static void sendToServerSideSuccess(BlockPos blockPos) {
        sendToServerSide(blockPos, 0, ItemTransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, ItemTransformType type, double number) {
        PacketDistributor.sendToServer(new ItemTransformDataOncePacket(blockPos, type, slot, number));
    }

    //after receive
    public static void handler(ItemTransformDataOncePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
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
