package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class GetShowBlockEntityItem extends AbstractMultiPurposeToolItem {
    public GetShowBlockEntityItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        MAX_FORMS=4;
    }
    private static final int
            GET_SHOWBLOCK=0,
            GET_MIXEDBLOCK=1,
            GET_ITEM=2,
            RESTORE=3;

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        getTag(handItemStack);
        switch (getForm()) {
            case GET_SHOWBLOCK:
            case GET_MIXEDBLOCK:{
                if (blockState.getBlock() instanceof ShowBlock) return InteractionResult.PASS;
                level.setBlock(blockPos, YuushyaRegistries.SHOW_BLOCK.get().defaultBlockState(), 35);
                ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
                showBlockEntity.setSlotBlockState(0, blockState);
                showBlockEntity.getTransformData(0).isShown = true;
                showBlockEntity.setSlot(getForm());
                showBlockEntity.saveChanged();
                return InteractionResult.SUCCESS;}
            case GET_ITEM:{
                if (!(blockState.getBlock() instanceof ShowBlock)) return InteractionResult.PASS;
                ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
                player.inventory.placeItemBackInInventory(level,showBlockEntity.getTransFormDataNow().blockState.getBlock().asItem().getDefaultInstance());
                return InteractionResult.SUCCESS;}
            case RESTORE:{
                if (!(blockState.getBlock() instanceof ShowBlock)) return InteractionResult.PASS;
                ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
                level.setBlock(blockPos, showBlockEntity.getTransFormDataNow().blockState, 35);
                return InteractionResult.SUCCESS;}
        }
        return InteractionResult.PASS;
    }

}
