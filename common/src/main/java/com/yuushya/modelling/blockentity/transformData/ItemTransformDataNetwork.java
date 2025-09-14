package com.yuushya.modelling.blockentity.transformData;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ItemTransformDataNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ITEM_TRANSFORM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "item_transform_data_packet");
    public static final StreamCodec<FriendlyByteBuf, ItemTransformDataOncePacket> STREAM_CODEC = CustomPacketPayload.codec(ItemTransformDataOncePacket::encoder, ItemTransformDataOncePacket::decoder);
    public static final CustomPacketPayload.Type<ItemTransformDataOncePacket> ITEM_TRANSFORM_DATA_ONCE_PACKET_TYPE = new CustomPacketPayload.Type<>(ITEM_TRANSFORM_DATA_PACKET_ID);

    public record ItemTransformDataOncePacket(
            BlockPos blockPos,
            ItemTransformType transformType,
            int slot,
            double number
    ) implements CustomPacketPayload {
        //buf -> pack
        public static ItemTransformDataOncePacket decoder(FriendlyByteBuf buf){
            return new ItemTransformDataOncePacket(
                    buf.readBlockPos(),
                    ItemTransformType.from(buf.readByte()),
                    buf.readByte(),
                    buf.readDouble()
            );
        }

        //pack -> buf
        public void encoder(FriendlyByteBuf buf){
            buf.writeBlockPos(blockPos);
            buf.writeByte(transformType.type);
            buf.writeByte(slot);
            buf.writeDouble(number);
        }

        //after receive
        public void handler(Supplier<NetworkManager.PacketContext> ctx){
            ctx.get().queue(() -> {
                Level level = ctx.get().getPlayer().level();
                if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(blockPos)) {
                    if(serverLevel.getBlockState(blockPos).getBlock() instanceof ItemBlock){
                        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
                        if (!(blockEntity instanceof ItemBlockEntity itemBlockEntity)) {return;}
                        transformType.modify(itemBlockEntity, slot, number);
                    }
                }
            });
        }

        @Override
        public CustomPacketPayload.Type<ItemTransformDataOncePacket> type() {
            return ITEM_TRANSFORM_DATA_ONCE_PACKET_TYPE;
        }
    }

    public static void sendToServerSideSuccess(BlockPos blockPos){
        sendToServerSide(blockPos, 0, ItemTransformType.SUCCESS, 0);
    }

    public static void sendToServerSide(BlockPos blockPos, int slot, ItemTransformType type, double number){
        NetworkManager.sendToServer(new ItemTransformDataOncePacket(blockPos, type, slot, number));
    }

    public static void registerServerSideReceiver(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ITEM_TRANSFORM_DATA_ONCE_PACKET_TYPE, STREAM_CODEC, (packet, context) -> {
            packet.handler(() -> context);
        });
    }
}