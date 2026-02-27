package com.yuushya.modelling.network;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.ITEM_STACK;

public class ItemStackPacket {
    private final BlockPos blockPos;
    private final int slot;
    private final ItemStack itemStack;

    public ItemStackPacket(BlockPos blockPos, int slot, ItemStack itemStack) {
        this.blockPos = blockPos;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public ItemStackPacket(FriendlyByteBuf buffer) {
        this.blockPos = buffer.readBlockPos();
        this.slot = buffer.readVarInt();
        this.itemStack = buffer.readItem();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.blockPos);
        buffer.writeVarInt(this.slot);
        buffer.writeItem(this.itemStack);
    }

    public static void handle(ItemStackPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ctx.get().getSender().level();
            if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(packet.blockPos)) {
                if (serverLevel.getBlockEntity(packet.blockPos) instanceof ItemBlockEntity itemBlockEntity) {
                    ITEM_STACK.modify(itemBlockEntity, packet.slot, packet.itemStack);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(BlockPos blockPos, int slot, ItemStack itemStack) {
        YuushyaModellingNetwork.INSTANCE.sendToServer(new ItemStackPacket(blockPos, slot, itemStack));
    }
}
