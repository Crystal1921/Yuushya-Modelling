package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.item.AbstractToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GuiItem  extends AbstractToolItem {
    public GuiItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if(level.isClientSide){
            if(blockState.getBlock() instanceof ShowBlock) {
                ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
                    Minecraft.getInstance().setScreen(
                            new ShowBlockScreen(showBlockEntity)
                    );
            }
        }
        return InteractionResult.SUCCESS;
    }
}
