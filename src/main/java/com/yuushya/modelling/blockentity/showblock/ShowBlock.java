package com.yuushya.modelling.blockentity.showblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.YuushyaNeoForge;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

public class ShowBlock extends AbstractTransformBlock {
    public ShowBlock(Properties properties, Integer tipLines) {
        super(properties.setId(ResourceKey.create(Registries.BLOCK, YuushyaNeoForge.id("show_block")))
                , tipLines);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof ShowBlockEntity showBlockEntity) {

            if (context.isHoldingItem(ItemRegistry.GUI_ITEM.get())) {
                showBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(ItemRegistry.ROT_TRANS_ITEM.get())) {
                showBlockEntity.setShowRotAxis();
                showBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.POS_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MICRO_POS_TRANS_ITEM.get())
            ) {
                showBlockEntity.setShowPosAxis();
                showBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.SLOT_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_SHOWBLOCK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_BLOCKSTATE_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.SCALE_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DEBUG_STICK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DESTROY_ITEM.get())
            ) {
                showBlockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ShowBlockEntity(blockPos, blockState);
    }

    @Override
    protected BlockState updateShape(BlockState stateIn, LevelReader worldIn, ScheduledTickAccess ticks, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        if (worldIn.getBlockEntity(currentPos) instanceof ShowBlockEntity showBlockEntity) {
            BlockState blockState = showBlockEntity.getTransformData(0).blockState;
            Block block = blockState.getBlock();

            if (facingState.getBlock() instanceof ShowBlock) {
                showBlockEntity.saveChanged();
                return stateIn;
            }
            if (!(block instanceof AirBlock)) {
                BlockState blockState1 = blockState.updateShape(worldIn, ticks, currentPos, facing, facingPos, facingState, random);
                if (!blockState1.isEmpty()) {
                    showBlockEntity.getTransformData(0).blockState = blockState1;
                    showBlockEntity.saveChanged();
                    return stateIn.setValue(POWERED, !stateIn.getValue(POWERED));
                }
            }
            showBlockEntity.saveChanged();
        }
        return stateIn.setValue(POWERED, !stateIn.getValue(POWERED));
    }


}
