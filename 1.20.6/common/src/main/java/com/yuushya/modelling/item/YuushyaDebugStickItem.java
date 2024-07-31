package com.yuushya.modelling.item;


import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;

public class YuushyaDebugStickItem extends AbstractToolItem {
    public YuushyaDebugStickItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    //对方块主手右键
    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        return !this.handleInteraction(player, level.getBlockState(blockPos), level, blockPos, true, handItemStack)
                ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    //对方块主手左键
    @Override
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        this.handleInteraction(player, blockState, level, blockPos, false, player.getItemInHand(InteractionHand.MAIN_HAND));
        return InteractionResult.PASS;
    }

    private boolean handleInteraction(Player player, BlockState stateClicked, LevelAccessor accessor, BlockPos pos, boolean shouldCycleState, ItemStack debugStack) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        }
        boolean isShowBlock = stateClicked.getBlock() instanceof ShowBlock;
        stateClicked = YuushyaUtils.getBlockState(stateClicked, accessor, pos);
        Holder<Block> holder = stateClicked.getBlockHolder();
        StateDefinition<Block, BlockState> stateDefinition = holder.value().getStateDefinition();
        Collection<Property<?>> collection = stateDefinition.getProperties();
        if (collection.isEmpty()) {
            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".empty", holder.getRegisteredName()), true);
            return false;
        }
        DebugStickState debugStickState = debugStack.get(DataComponents.DEBUG_STICK_STATE);
        if (debugStickState == null) {
            return false;
        }
        Property<?> property = debugStickState.properties().get(holder);
        if (shouldCycleState) {
            if (property == null) {
                property = collection.iterator().next();
            }
            BlockState blockStateNew = YuushyaBlockStates.cycleState(stateClicked, property, player.isSecondaryUseActive());
            if (isShowBlock) {
                ShowBlockEntity showBlockEntity = (ShowBlockEntity) accessor.getBlockEntity(pos);
                showBlockEntity.setSlotBlockState(0, blockStateNew);
                showBlockEntity.saveChanged();
            } else {
                accessor.setBlock(pos, blockStateNew, 18);
            }
            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".update", property.getName(), getNameHelper(blockStateNew, property)), true);
        } else {
            property = YuushyaBlockStates.getRelative(collection, property, player.isSecondaryUseActive());
            debugStack.set(DataComponents.DEBUG_STICK_STATE, debugStickState.withProperty(holder, property));
            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".select", property.getName(), getNameHelper(stateClicked, property)), true);
        }
        return true;
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState blockState, Property<T> property) {
        return property.getName(blockState.getValue(property));
    }
}
