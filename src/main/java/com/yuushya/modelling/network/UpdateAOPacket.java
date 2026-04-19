package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_SPECIAL_RENDER;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.FULL_BLOCK;

public record UpdateAOPacket(boolean enableAO, boolean fullBlock, boolean enableSpecialRender, BlockPos blockPos) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, UpdateAOPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            UpdateAOPacket::enableAO,
            ByteBufCodecs.BOOL,
            UpdateAOPacket::fullBlock,
            ByteBufCodecs.BOOL,
            UpdateAOPacket::enableSpecialRender,
            BlockPos.STREAM_CODEC,
            UpdateAOPacket::blockPos,
            UpdateAOPacket::new
    );
    public static final Type<UpdateAOPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "diable_ao_packet"));

    public static void handler(UpdateAOPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
                if (blockEntity instanceof ItemBlockEntity) {
                    level.setBlockAndUpdate(blockEntity.getBlockPos(), blockEntity.getBlockState()
                            .setValue(ENABLE_AO, packet.enableAO)
                            .setValue(FULL_BLOCK, packet.fullBlock)
                            .setValue(ENABLE_SPECIAL_RENDER, packet.enableSpecialRender));
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


}
