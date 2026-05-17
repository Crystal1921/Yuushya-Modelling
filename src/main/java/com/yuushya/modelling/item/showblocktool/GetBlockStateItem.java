package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.registries.DataComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GetBlockStateItem extends AbstractToolItem {
    private BlockState blockState;

    public GetBlockStateItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        blockState = Blocks.AIR.defaultBlockState();
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockStateTarget, Level level, BlockPos blockPos, ItemStack handItemStack) {
        //右手右键复制状态，以及清空展示方块内的东西//with main hand right-click can read
        getTag(handItemStack);
        if (blockStateTarget.getBlock() instanceof ShowBlock) {
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            BlockState blockStateShowBlock = showBlockEntity.getTransFormDataNow().blockState;
            if (!(blockStateShowBlock.getBlock() instanceof AirBlock)) {
                blockStateTarget = blockStateShowBlock;
                showBlockEntity.removeTransFormDataNow();
                showBlockEntity.saveChanged();
            } else {
                player.sendOverlayMessage(Component.translatable(this.getDescriptionId() + ".mainhand.pass"));
                return InteractionResult.PASS;
            }
        }
        blockState = blockStateTarget;
        setTag(handItemStack);
        player.sendOverlayMessage(Component.translatable(this.getDescriptionId() + ".mainhand.success"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult inOffHandRightClickOnBlock(Player player, BlockState blockStateTarget, Level level, BlockPos blockPos, ItemStack handItemStack) {
        //左手右键放置状态到展示方块里//with off hand right-click can put blockstate to showblock
        getTag(handItemStack);
        if (blockState.getBlock() instanceof AirBlock) {
            player.sendOverlayMessage(Component.translatable(this.getDescriptionId() + ".offhand.fail"));
            return InteractionResult.SUCCESS;
        }
        if (blockStateTarget.getBlock() instanceof ShowBlock) {
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            showBlockEntity.setSlotBlockStateNow(blockState);
            showBlockEntity.setSlotShown(showBlockEntity.getSlot(), true);
            showBlockEntity.saveChanged();
            player.sendOverlayMessage(Component.translatable(this.getDescriptionId() + ".offhand.success"));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    //method for readNbt and writeNbt
    public void getTag(ItemStack itemStack) {
        blockState = itemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
    }

    public void setTag(ItemStack itemStack) {
        itemStack.set(DataComponentRegistry.BLOCKSTATE.get(), blockState);
    }

}
