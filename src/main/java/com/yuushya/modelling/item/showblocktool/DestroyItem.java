package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.utils.DeprecatedMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class DestroyItem extends AbstractMultiPurposeToolItem {
    public DestroyItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    public static void saveToItem(ItemStack itemStack, BlockState blockState, HolderLookup.Provider registries) {
        List<ITransformDataProvider> transformDataList = new ArrayList<>();
        TransformBlockData data = new TransformBlockData();
        data.blockState = blockState;
        data.isShown = true;
        transformDataList.add(data);
        saveToItem(itemStack, transformDataList, registries);
    }

    //TODO 不知道这个IndexedPathElement怎么办
    public static void saveToItem(ItemStack itemStack, List<? extends ITransformDataProvider> transformDataList, HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(new ProblemReporter.IndexedPathElement(1), Yuushya.SLF_LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            ITransformDataInventory.saveAdditional(output, transformDataList, registries);
            BlockItem.setBlockEntityData(itemStack, BlockEntityRegistry.SHOW_BLOCK_ENTITY.get(), output);
        }
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        ItemStack offhandItem = player.getOffhandItem();

        if (!(offhandItem.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        if (!(blockItem.getBlock() instanceof AbstractTransformBlock)) {
            return InteractionResult.PASS;
        }

        if (blockState.getBlock() instanceof AbstractTransformBlock) {
            if (level.getBlockEntity(blockPos) instanceof AbstractTransformBlockEntity showBlockEntity) {
                DeprecatedMethod.saveToItem(showBlockEntity, offhandItem, level.registryAccess());
                showBlockEntity.writeBlockState(offhandItem, blockState);
            }
        } else {
            saveToItem(offhandItem, blockState, level.registryAccess());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (blockState.getBlock() instanceof AbstractTransformBlock showBlock && level.getBlockEntity(blockPos) instanceof AbstractTransformBlockEntity showBlockEntity) {
            if (!level.isClientSide()) {
                ItemStack itemStack = new ItemStack(showBlock);
                DeprecatedMethod.saveToItem(showBlockEntity, itemStack, level.registryAccess());
                showBlockEntity.writeBlockState(itemStack, blockState);
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
                if (!player.isCreative()) handItemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                ItemEntity itemEntity = new ItemEntity(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }

        }
        return InteractionResult.SUCCESS;
    }
}
