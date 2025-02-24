package com.yuushya.modelling.blockentity;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
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

public class TransformDataNetwork{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation TRANSFORM_DATA_PACKET_ID = ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "transform_data_packet");
    public static final StreamCodec<FriendlyByteBuf, TransformDataOncePacket> STREAM_CODEC = CustomPacketPayload.codec(TransformDataOncePacket::encoder, TransformDataOncePacket::decoder);
    public static final CustomPacketPayload.Type<TransformDataOncePacket> TRANSFORM_DATA_ONCE_PACKET_TYPE = new CustomPacketPayload.Type<>(TRANSFORM_DATA_PACKET_ID);

    public record TransformDataOncePacket(
            BlockPos blockPos,
            TransformType transformType,
            int slot,
            double number
    )   implements CustomPacketPayload {
        //buf -> pack
        public static TransformDataOncePacket decoder(FriendlyByteBuf buf){
            return new TransformDataOncePacket(
                    buf.readBlockPos(),
                    TransformType.from(buf.readByte()),
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
                    if(serverLevel.getBlockState(blockPos).getBlock() instanceof ShowBlock){
                        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
                        if (!(blockEntity instanceof ShowBlockEntity showBlockEntity)) {return;}
                        transformType.modify(showBlockEntity,slot,number);
                    }
                }
            });
        }

        @Override
        public CustomPacketPayload.Type<TransformDataOncePacket> type() {
            return TRANSFORM_DATA_ONCE_PACKET_TYPE;
        }

    }


    public static void sendToServerSideSuccess(BlockPos blockPos){
        sendToServerSide(blockPos,0,TransformType.SUCCESS,0);
    }

    //architectury提供的另一种风格的api
    public static void sendToServerSide(BlockPos blockPos, int slot, TransformType type, double number){
        //FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        //new TransformDataOncePacket(blockPos,type,slot,number).encoder(buf);
        NetworkManager.sendToServer(new TransformDataOncePacket(blockPos,type,slot,number));
    }

    public static void registerServerSideReceiver(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRANSFORM_DATA_ONCE_PACKET_TYPE,STREAM_CODEC, (packet, context) -> {
            packet.handler(()->context);
        });
    }




}
