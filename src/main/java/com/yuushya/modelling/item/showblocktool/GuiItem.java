package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GuiItem extends AbstractToolItem {
    public GuiItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide()) {
            ClientMethod.openGuiScreen(player, blockState, level, blockPos, handItemStack);
        }
        return InteractionResult.SUCCESS;
    }

}
