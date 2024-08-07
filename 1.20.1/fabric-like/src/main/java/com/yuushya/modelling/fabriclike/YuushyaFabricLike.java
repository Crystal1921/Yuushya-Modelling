package com.yuushya.modelling.fabriclike;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;

import static com.yuushya.modelling.blockentity.TransformDataNetwork.updateTransformData;
import static com.yuushya.modelling.registries.YuushyaRegistries.YUUSHYA_MODELLING;

public class YuushyaFabricLike {
    public static void init(){
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties(), 3));
        Yuushya.init();

        //ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> CollisionFileReader.readAllFileSelf());
    }
}

//        ServerPlayNetworking.registerGlobalReceiver(TransformDataNetwork.TRANSFORM_DATA_PACKET_ID, (server, player, handler, buf, responseSender) -> {
//            BlockPos blockPos = buf.readBlockPos();
//            TransformDataNetwork.TransformType transformType = TransformDataNetwork.TransformType.from(buf.readByte());
//            int slot = buf.readByte();
//            double number = buf.readDouble();
//            server.execute(()->{
//                ServerLevel level = player.serverLevel();
//                BlockEntity blockEntity = level.getBlockEntity(blockPos);
//
//                if (!(blockEntity instanceof ShowBlockEntity showBlockEntity)) {
//                    return;
//
//                }
//                else{
//                    updateTransformData(showBlockEntity,slot,transformType,number);
//                }
//            });
//        });