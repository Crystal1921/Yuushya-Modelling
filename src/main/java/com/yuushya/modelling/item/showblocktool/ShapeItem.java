package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.VoxelShapeSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShapeItem extends AbstractMultiPurposeToolItem {
    public ShapeItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    //对方块主手右键
    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (handItemStack.has(DataComponentRegistry.SHAPE_DATA)) {
            CompoundTag compoundTag = handItemStack.get(DataComponentRegistry.SHAPE_DATA);
            if (compoundTag == null) {
                return InteractionResult.FAIL;
            }
            VoxelShape voxelShape = VoxelShapeSerializer.deserializeVoxelShape(compoundTag);
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof AbstractTransformBlockEntity transformBlockEntity) {
                transformBlockEntity.setCustomShape(voxelShape);
                transformBlockEntity.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    //对方块主手左键
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        VoxelShape collisionShape = blockState.getCollisionShape(level, blockPos);
        CompoundTag compoundTag = VoxelShapeSerializer.serializeVoxelShape(collisionShape);
        handItemStack.set(DataComponentRegistry.SHAPE_DATA, compoundTag);
        player.displayClientMessage(Component.literal("Save Successfully"), true);
        return InteractionResult.SUCCESS;
    }
}
