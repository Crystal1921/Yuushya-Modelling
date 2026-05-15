package com.yuushya.modelling.blockentity.textblock;

import com.yuushya.modelling.YuushyaNeoForge;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public class TextBlock extends AbstractTransformBlock {
    public TextBlock(Properties properties, Integer tipLines) {
        super(properties.setId(ResourceKey.create(Registries.BLOCK, YuushyaNeoForge.id("text_block"))), tipLines);
    }

    @Override
    public @org.jspecify.annotations.Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return createCookTicker(level, type, BlockEntityRegistry.TEXT_BLOCK_ENTITY.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof TextBlockEntity textBlockEntity) {

            if (context.isHoldingItem(ItemRegistry.GUI_ITEM.get())) {
                textBlockEntity.triggerShowFrame();
            } else if (context.isHoldingItem(ItemRegistry.ROT_TRANS_ITEM.get())) {
                textBlockEntity.triggerShowAxis();
                textBlockEntity.triggerShowText();
            } else if (context.isHoldingItem(ItemRegistry.POS_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MICRO_POS_TRANS_ITEM.get())
            ) {
                textBlockEntity.triggerShowAxis();
                textBlockEntity.triggerShowText();
            } else if (context.isHoldingItem(ItemRegistry.SLOT_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_SHOWBLOCK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_BLOCKSTATE_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.SCALE_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DEBUG_STICK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DESTROY_ITEM.get())
            ) {
                textBlockEntity.triggerShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TextBlockEntity(blockPos, blockState);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack itemStack = new ItemStack(this);
        BlockItemStateProperties stateProperties = itemStack.get(DataComponents.BLOCK_STATE);
        if (stateProperties == null) stateProperties = BlockItemStateProperties.EMPTY;
        BlockShape value = stateProperties.get(SHAPES);
        if (value == null) value = BlockShape.BLOCK;
        itemStack.set(DataComponents.BLOCK_STATE, stateProperties.with(SHAPES, value));
        return itemStack;
    }
}
