package com.yuushya.modelling.blockentity.showblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
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
        super(properties, tipLines);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof ShowBlockEntity showBlockEntity) {

            if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "gui_item")))) {
                showBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "rot_trans_item")))) {
                showBlockEntity.setShowRotAxis();
                showBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "pos_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "micro_pos_trans_item")))
            ) {
                showBlockEntity.setShowPosAxis();
                showBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "slot_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "get_showblock_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "move_transformdata_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "get_blockstate_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "scale_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "debug_stick_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "destroy_item")))
            ) {
                showBlockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    @NotNull
    public VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(SHAPES).voxelShape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ShowBlockEntity(blockPos, blockState);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (worldIn.getBlockEntity(currentPos) instanceof ShowBlockEntity showBlockEntity) {
            BlockState blockState = showBlockEntity.getTransformData(0).blockState;
            Block block = blockState.getBlock();

            if (facingState.getBlock() instanceof ShowBlock) {
                showBlockEntity.saveChanged();
                return stateIn;
            }
            if (!(block instanceof AirBlock)) {
                BlockState blockState1 = blockState.updateShape(facing, facingState, worldIn, currentPos, facingPos);
                if (!blockState1.isAir()) {
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
