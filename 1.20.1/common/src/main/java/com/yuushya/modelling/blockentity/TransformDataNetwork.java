package com.yuushya.modelling.blockentity;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Supplier;

public class TransformDataNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation TRANSFORM_DATA_PACKET_ID = new ResourceLocation(Yuushya.MOD_ID_USED, "transform_data_packet");
    public static final NetworkChannel INSTANCE = NetworkChannel.create(TRANSFORM_DATA_PACKET_ID);
    public enum TransformType{
        POS_X(0),POS_Y(1),POS_Z(2),
        ROT_X(3),ROT_Y(4),ROT_Z(5),
        SCALA_X(6),SCALA_Y(7),SCALA_Z(8),
        BLOCK_STATE(9),
        SHOWN(10),
        SUCCESS(11), FAIL(12);

        public final int type;
        TransformType(int i){ type = i;}
        public static TransformType from(int i){
            return switch (i){
                case 0 -> POS_X; case 1 -> POS_Y; case 2 -> POS_Z;
                case 3 -> ROT_X; case 4 -> ROT_Y; case 5 -> ROT_Z;
                case 6 -> SCALA_X; case 7 -> SCALA_Y; case 8 -> SCALA_Z;
                case 9 -> BLOCK_STATE;
                case 10 -> SHOWN;
                case 11 -> SUCCESS;
                default -> FAIL;
            };
        }
    }

    public static void updateTransformData(ShowBlockEntity showBlockEntity, int slot, TransformType type, double number){
        if(type == TransformType.SUCCESS){
            showBlockEntity.saveChanged();
            return;
        }
        showBlockEntity.setSlot(slot);
        TransformData transformData = showBlockEntity.getTransformData(slot);
        switch (type){
            case POS_X -> transformData.pos.set(number,transformData.pos.y(),transformData.pos.z());
            case POS_Y -> transformData.pos.set(transformData.pos.x(),number,transformData.pos.z());
            case POS_Z -> transformData.pos.set(transformData.pos.x(),transformData.pos.y(),number);
            case ROT_X -> transformData.rot.set(number,transformData.rot.y(),transformData.rot.z());
            case ROT_Y -> transformData.rot.set(transformData.rot.x(),number,transformData.rot.z());
            case ROT_Z -> transformData.rot.set(transformData.rot.x(),transformData.rot.y(),number);
            case SCALA_X -> transformData.scales.set(number,transformData.scales.y(),transformData.scales.z());
            case SCALA_Y -> transformData.scales.set(transformData.scales.x(),number,transformData.scales.z());
            case SCALA_Z -> transformData.scales.set(transformData.scales.x(),transformData.scales.y(),number);
            case BLOCK_STATE -> transformData.blockState = Block.stateById((int) Math.round(number));
            case SHOWN -> transformData.isShown = number != 0;
            //case SUCCESS -> showBlockEntity.saveChanged();
            case FAIL -> {
            }
        }
    }



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
                        updateTransformData(showBlockEntity,slot,transformType,number);
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
