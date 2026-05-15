package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.YuushyaNeoForge;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ItemBlock extends AbstractTransformBlock {
    public ItemBlock(Properties properties, Integer tipLines) {
        super(properties.setId(ResourceKey.create(Registries.BLOCK, YuushyaNeoForge.id("item_block"))), tipLines);
    }

    @Override
    public @org.jspecify.annotations.Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return createCookTicker(level, type, BlockEntityRegistry.ITEM_BLOCK_ENTITY.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof ItemBlockEntity itemBlockEntity) {

            if (context.isHoldingItem(ItemRegistry.GUI_ITEM.get())) {
                itemBlockEntity.triggerShowFrame();
            } else if (context.isHoldingItem(ItemRegistry.ROT_TRANS_ITEM.get())) {
                itemBlockEntity.triggerShowAxis();
                itemBlockEntity.triggerShowText();
            } else if (context.isHoldingItem(ItemRegistry.POS_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MICRO_POS_TRANS_ITEM.get())
            ) {
                itemBlockEntity.triggerShowAxis();
                itemBlockEntity.triggerShowText();
            } else if (context.isHoldingItem(ItemRegistry.SLOT_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_SHOWBLOCK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_BLOCKSTATE_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.SCALE_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DEBUG_STICK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DESTROY_ITEM.get())
            ) {
                itemBlockEntity.triggerShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ItemBlockEntity(blockPos, blockState);
    }
}
