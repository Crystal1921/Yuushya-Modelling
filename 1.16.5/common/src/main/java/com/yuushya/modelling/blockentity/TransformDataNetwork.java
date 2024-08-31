package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;

import io.netty.buffer.Unpooled;
import me.shedaniel.architectury.networking.NetworkChannel;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.function.Supplier;

public class TransformDataNetwork {
    public static final ResourceLocation TRANSFORM_DATA_PACKET_ID = new ResourceLocation(Yuushya.MOD_ID_USED, "transform_data_packet");
    public static final NetworkChannel INSTANCE = NetworkChannel.create(TRANSFORM_DATA_PACKET_ID);

    public static final class TransformDataOncePacket {
        private final BlockPos blockPos;
        private final TransformType transformType;
        private final int slot;
        private final double number;

        public TransformDataOncePacket(
                BlockPos blockPos,
                TransformType transformType,
                int slot,
                double number
        ) {
            this.blockPos = blockPos;
            this.transformType = transformType;
            this.slot = slot;
            this.number = number;
        }

        //buf -> pack
            public static TransformDataOncePacket decoder(FriendlyByteBuf buf) {
                return new TransformDataOncePacket(
                        buf.readBlockPos(),
                        TransformType.from(buf.readByte()),
                        buf.readByte(),
                        buf.readDouble()
                );
            }

            //pack -> buf
            public void encoder(FriendlyByteBuf buf) {
                buf.writeBlockPos(blockPos);
                buf.writeByte(transformType.type);
                buf.writeByte(slot);
                buf.writeDouble(number);
            }

            //after receive
            public void handler(Supplier<NetworkManager.PacketContext> ctx) {
                ctx.get().queue(() -> {
                    Level level = ctx.get().getPlayer().level;
                    if (level instanceof ServerLevel && level.hasChunkAt(blockPos)) {
                        if (level.getBlockState(blockPos).getBlock() instanceof ShowBlock) {
                            BlockEntity blockEntity = level.getBlockEntity(blockPos);
                            if (!(blockEntity instanceof ShowBlockEntity)) {
                                return;
                            }
                            transformType.modify((ShowBlockEntity) blockEntity, slot, number);
                        }
                    }
                });
            }

        public BlockPos blockPos() {
            return blockPos;
        }

        public TransformType transformType() {
            return transformType;
        }

        public int slot() {
            return slot;
        }

        public double number() {
            return number;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            TransformDataOncePacket that = (TransformDataOncePacket) obj;
            return Objects.equals(this.blockPos, that.blockPos) &&
                    Objects.equals(this.transformType, that.transformType) &&
                    this.slot == that.slot &&
                    Double.doubleToLongBits(this.number) == Double.doubleToLongBits(that.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockPos, transformType, slot, number);
        }

        @Override
        public String toString() {
            return "TransformDataOncePacket[" +
                    "blockPos=" + blockPos + ", " +
                    "transformType=" + transformType + ", " +
                    "slot=" + slot + ", " +
                    "number=" + number + ']';
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
