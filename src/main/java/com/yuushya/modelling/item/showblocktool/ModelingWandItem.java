package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.modeling.ModelingScreen;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 建模工作台入口：对方块右键打开 {@link ModelingScreen}。
 * <p>
 * 接受 {@link ShowBlockEntity} 与 {@link ItemBlockEntity}，其它方块一律 {@code PASS}，
 * 不吞掉点击。旧的 GuiItem 仍然打开原来的三个界面，两者并存。
 */
public class ModelingWandItem extends AbstractToolItem {

    public ModelingWandItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level,
                                                         BlockPos blockPos, ItemStack handItemStack) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (level.isClientSide) {
            if (blockEntity instanceof ShowBlockEntity showBlock) {
                ClientMethod.openModelingScreen(showBlock);
                return InteractionResult.SUCCESS;
            }
            if (blockEntity instanceof ItemBlockEntity itemBlock) {
                ClientMethod.openModelingScreen(itemBlock);
                return InteractionResult.SUCCESS;
            }
        } else if (blockEntity instanceof ShowBlockEntity || blockEntity instanceof ItemBlockEntity) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult inOffHandRightClickOnBlock(Player player, BlockState blockState, Level level,
                                                        BlockPos blockPos, ItemStack handItemStack) {
        return inMainHandRightClickOnBlock(player, blockState, level, blockPos, handItemStack);
    }
}
