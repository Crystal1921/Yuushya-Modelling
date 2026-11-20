package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 碰撞箱雕刻工具：右键变换方块打开雕刻界面。
 * <p>
 * 只接受 {@link AbstractTransformBlockEntity}（展示方块 / 物品方块 / 文本方块），
 * 其它方块返回 {@code PASS}，不吞掉点击。
 */
public class CollisionCarverItem extends AbstractToolItem {

    public CollisionCarverItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level,
                                                         BlockPos blockPos, ItemStack handItemStack) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof AbstractTransformBlockEntity transform)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            ClientMethod.openCollisionEditor(null, transform);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult inOffHandRightClickOnBlock(Player player, BlockState blockState, Level level,
                                                        BlockPos blockPos, ItemStack handItemStack) {
        return inMainHandRightClickOnBlock(player, blockState, level, blockPos, handItemStack);
    }
}