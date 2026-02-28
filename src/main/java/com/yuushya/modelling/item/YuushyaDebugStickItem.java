package com.yuushya.modelling.item;


import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    public static <T extends Comparable<T>> String getNameHelper(BlockState blockState, Property<T> property) {
        return property.getName(blockState.getValue(property));
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
            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".empty", holder.get().getDescriptionId()), true);
            return false;
        }

        // Get or create the debug stick state from NBT
        CompoundTag tag = debugStack.getOrCreateTag();
        Property<?> property = null;
        if (tag.contains("DebugState")) {
            CompoundTag debugState = tag.getCompound("DebugState");
            HolderGetter<Block> blockGetter =
                    accessor.registryAccess().lookupOrThrow(Registries.BLOCK);

            Block block = NbtUtils.readBlockState(blockGetter,debugState.getCompound("Block")).getBlock();
            if (block == holder.value()) {
                String propertyName = debugState.getString("Property");
                property = stateDefinition.getProperty(propertyName);
            }
        }

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

            // Save the new property to NBT
            CompoundTag debugState = new CompoundTag();
            debugState.put("Block", NbtUtils.writeBlockState(stateClicked));
            debugState.putString("Property", property.getName());
            tag.put("DebugState", debugState);

            player.displayClientMessage(Component.translatable(this.getDescriptionId() + ".select", property.getName(), getNameHelper(stateClicked, property)), true);
        }
        return true;
    }
}
