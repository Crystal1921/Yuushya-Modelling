package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.gui.engrave.EngraveMenu;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EngraveItem extends AbstractMultiPurposeToolItem {
    public EngraveItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    public static EngraveMenu getEngraveMenu(int i, Inventory inventory, Level level, BlockPos pos, ItemStack itemStack) {
        return new EngraveMenu(i, inventory, ContainerLevelAccess.create(level, pos)) {
            {
                this.getSlot(0).set(itemStack);
            }
        };
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack offhandItem = player.getOffhandItem();
        if (offhandItem.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShowBlock || block instanceof ItemBlock || block instanceof TextBlock) {
                ItemStack itemStack = offhandItem.copy();
                if (player.isCreative()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }
                else {
                    player.getItemInHand(InteractionHand.OFF_HAND).setCount(0);
                }
                player.openMenu(getMenuProvider(level, player.blockPosition(), itemStack));
                player.awardStat(Stats.INTERACT_WITH_STONECUTTER);//player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult inMainHandRightClickInAir(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        return inMainHandRightClickOnBlock(player, blockState, level, blockPos, handItemStack);
    }

    public MenuProvider getMenuProvider(Level level, BlockPos pos, ItemStack itemStack) {
        return new SimpleMenuProvider((i, inventory, _player) -> getEngraveMenu(i, inventory, level, pos, itemStack), Component.literal(descriptionId));
    }
}
