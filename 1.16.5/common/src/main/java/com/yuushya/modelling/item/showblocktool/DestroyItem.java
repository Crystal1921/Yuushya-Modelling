package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.utils.YuushyaUtils.BLOCK_ENTITY_TAG;
import static net.minecraft.world.level.block.Block.getId;

public class DestroyItem extends AbstractMultiPurposeToolItem{
    public DestroyItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        ItemStack offhandItem = player.getOffhandItem();
        if(offhandItem.getItem() instanceof BlockItem){
            if(((BlockItem)offhandItem.getItem()).getBlock() instanceof ShowBlock){
                if(blockState.getBlock() instanceof ShowBlock){
                    ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
                    CompoundTag compoundTag = showBlockEntity.save(new CompoundTag());
                    if (!compoundTag.isEmpty()) {
                        offhandItem.addTagElement(BLOCK_ENTITY_TAG, compoundTag);
                    }
                }
                else{
                    saveToItem(offhandItem,blockState);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if(blockState.getBlock() instanceof ShowBlock) {
            ShowBlock showBlock = (ShowBlock) blockState.getBlock();
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            if(!level.isClientSide){
                ItemStack itemStack = new ItemStack(showBlock);
                CompoundTag compoundTag = showBlockEntity.save(new CompoundTag());
                if (!compoundTag.isEmpty()) {
                    itemStack.addTagElement("BlockEntityTag", compoundTag);
                }
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
                if(!player.isCreative()) handItemStack.hurtAndBreak(1, player, livingEntity -> livingEntity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                ItemEntity itemEntity = new ItemEntity(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }

        }
        return InteractionResult.SUCCESS;
    }

    public static void saveToItem(ItemStack itemStack,BlockState blockState){
        List<TransformData> transformDataList = new ArrayList<>();
        TransformData data =  new TransformData();
        data.blockState = blockState;
        data.isShown = true;
        transformDataList.add(data);
        saveToItem(itemStack,transformDataList);
    }

    public static void saveToItem(ItemStack itemStack,List<TransformData> transformDataList){
        CompoundTag compoundTag = new CompoundTag();
        ITransformDataInventory.saveAdditional(compoundTag, transformDataList);
        setBlockEntityData(itemStack, YuushyaRegistries.SHOW_BLOCK_ENTITY.get(), compoundTag);
    }

    public static void setBlockEntityData(ItemStack stack, BlockEntityType<?> blockEntityType, CompoundTag blockEntityData) {
        if (blockEntityData.isEmpty()) {
            stack.removeTagKey(BLOCK_ENTITY_TAG);
        } else {
            blockEntityData.putString("id", BlockEntityType.getKey(blockEntityType).toString());
            stack.addTagElement(BLOCK_ENTITY_TAG, blockEntityData);
        }
    }
}
