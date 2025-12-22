package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.registries.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GuiItem extends AbstractToolItem {
    public GuiItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide) {
            openGuiScreen(player, blockState, level, blockPos, handItemStack);
        }
        return InteractionResult.SUCCESS;
    }

    public void openGuiScreen(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        ItemStack newItemStack = player.getItemInHand(InteractionHand.OFF_HAND);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        BlockState newBlockState = null;

        if (newItemStack.getItem() instanceof GetBlockStateItem) {
            newBlockState = newItemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
        } else if (newItemStack.getItem() instanceof BlockItem item) {
            newBlockState = item.getBlock().defaultBlockState();
        }
        if (!newItemStack.isEmpty()) {
            newItemStack = newItemStack.copy();
        }

        if (blockEntity instanceof ShowBlockEntity showBlockEntity) {
            Minecraft.getInstance().setScreen(
                    new ShowBlockScreen(showBlockEntity, newBlockState)
            );
        } else if (blockEntity instanceof ItemBlockEntity itemBlockEntity) {
            Minecraft.getInstance().setScreen(
                    new ItemBlockScreen(itemBlockEntity, newItemStack)
            );
        } else if (blockEntity instanceof TextBlockEntity textBlockEntity) {
            // For TextBlockEntity, we can optionally pass initial text lines
            List<String> newTextLines = new ArrayList<>();
            Minecraft.getInstance().setScreen(
                    new TextBlockScreen(textBlockEntity, newTextLines)
            );
        }
    }
}
