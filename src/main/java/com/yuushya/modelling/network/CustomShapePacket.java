package com.yuushya.modelling.network;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.utils.VoxelShapeSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
import static com.yuushya.modelling.blockentity.BlockShape.CUSTOM;

/**
 * 碰撞箱雕刻结果同步：把客户端雕好的形状写回服务端的方块实体。
 * <p>
 * 形状本身是服务端数据（碰撞判定、存盘都在那边算），只在客户端改是没用的，
 * 所以编辑界面点「应用」时发这个包。盒子列表用 {@link VoxelShapeSerializer} 的 NBT 传递。
 */
public record CustomShapePacket(BlockPos blockPos, CompoundTag shape, boolean setCustomShape)
        implements CustomPacketPayload {

    public static final ResourceLocation CUSTOM_SHAPE_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "custom_shape_packet");
    public static final Type<CustomShapePacket> TYPE = new Type<>(CUSTOM_SHAPE_PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, CustomShapePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CustomShapePacket::blockPos,
            ByteBufCodecs.COMPOUND_TAG,
            CustomShapePacket::shape,
            ByteBufCodecs.BOOL,
            CustomShapePacket::setCustomShape,
            CustomShapePacket::new
    );

    public static void sendToServerSide(BlockPos blockPos, VoxelShape shape, boolean setCustomShape) {
        PacketDistributor.sendToServer(new CustomShapePacket(blockPos,
                VoxelShapeSerializer.serializeVoxelShape(shape), setCustomShape));
    }

    public static void handler(CustomShapePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (!(level instanceof ServerLevel serverLevel) || !serverLevel.hasChunkAt(packet.blockPos)) {
                return;
            }
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.blockPos);
            if (!(blockEntity instanceof AbstractTransformBlockEntity transformBlockEntity)) {
                return;
            }
            transformBlockEntity.setCustomShape(VoxelShapeSerializer.deserializeVoxelShape(packet.shape));
            if (packet.setCustomShape) {
                            // 与形状工具一致：只改状态并同步客户端，不触发邻居更新
                serverLevel.setBlock(packet.blockPos,
                                    transformBlockEntity.getBlockState().setValue(SHAPES, CUSTOM),
                                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
            // setChanged + 广播给客户端，让客户端方块实体也拿到新形状
            transformBlockEntity.saveChanged();
        });
    }

    @Override
    public @NotNull Type<CustomShapePacket> type() {
        return TYPE;
    }
}