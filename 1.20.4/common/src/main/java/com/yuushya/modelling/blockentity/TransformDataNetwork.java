package com.yuushya.modelling.blockentity;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class TransformDataNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation TRANSFORM_DATA_PACKET_ID = new ResourceLocation(Yuushya.MOD_ID_USED, "transform_data_packet");
    public static final NetworkChannel INSTANCE = NetworkChannel.create(TRANSFORM_DATA_PACKET_ID);

    public record TransformDataOncePacket(
            BlockPos blockPos,
            TransformType transformType,
            int slot,
            double number
    ){
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
    }


    public static void sendToServerSide(BlockPos blockPos, int slot, TransformType type, double number) {
        INSTANCE.sendToServer(new TransformDataOncePacket(blockPos,type,slot,number));
    }

    public static void sendToServerSideSuccess(BlockPos blockPos){
        sendToServerSide(blockPos,0,TransformType.SUCCESS,0);
    }

    public static void registerChannel(){
        INSTANCE.register(TransformDataOncePacket.class,TransformDataOncePacket::encoder,TransformDataOncePacket::decoder,TransformDataOncePacket::handler);
    }

    //architectury提供的另一种风格的api
    public static void sendToServerSideLegacy(BlockPos blockPos, int slot, TransformType type, double number){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new TransformDataOncePacket(blockPos,type,slot,number).encoder(buf);
        NetworkManager.sendToServer(TRANSFORM_DATA_PACKET_ID,buf);
    }

    public static void registerServerSideReceiverLegacy(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRANSFORM_DATA_PACKET_ID, (buf, context) -> {
            TransformDataOncePacket packet = TransformDataOncePacket.decoder(buf);
            packet.handler(()->context);
        });
    }




}
