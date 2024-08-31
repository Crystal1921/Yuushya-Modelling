package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GuiItem  extends AbstractToolItem {
    public GuiItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if(level.isClientSide) {
            openGuiScreen(player, blockState, level, blockPos, handItemStack);
        }
        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    public void openGuiScreen(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        BlockState newBlockState = null;
        for(ItemStack itemStack: player.getHandSlots()){
            if(itemStack.getItem() instanceof GetBlockStateItem){
                CompoundTag compoundTag = itemStack.getOrCreateTag();
                if(compoundTag.contains("BlockState")){
                    newBlockState = NbtUtils.readBlockState(compoundTag.getCompound("BlockState"));
                }
            }
            else if(itemStack.getItem() instanceof BlockItem){
                BlockItem item = (BlockItem) itemStack.getItem();
                newBlockState = item.getBlock().defaultBlockState();
            }
        }

        if(blockState.getBlock() instanceof ShowBlock) {
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            Minecraft.getInstance().setScreen(
                    new ShowBlockScreen(showBlockEntity,newBlockState)
            );
        }
    }
}
