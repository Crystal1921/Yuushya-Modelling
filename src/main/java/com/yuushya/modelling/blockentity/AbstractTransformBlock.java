package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.AbstractYuushyaBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.LIT;
import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

/**
 * Abstract base class for transform blocks that provides common functionality
 * for both ShowBlock and ItemBlock implementations.
 */
public abstract class AbstractTransformBlock extends AbstractYuushyaBlock implements EntityBlock {
    public static final BooleanProperty ENABLE_AO = BooleanProperty.create("enable_ao");

    public static final Boolean DEFAULT_ENABLE_AO = false;

    public AbstractTransformBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        this.registerDefaultState(defaultBlockState()
                .setValue(POWERED, false)
                .setValue(LIT, 0)
                .setValue(HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(ENABLE_AO, DEFAULT_ENABLE_AO));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(LIT).add(POWERED).add(HORIZONTAL_FACING).add(SHAPES).add(ENABLE_AO);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        if (blockPlaceContext.getPlayer() != null && 
            blockPlaceContext.getPlayer().isHolding(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "rot_trans_item")))) {
            BlockState blockState = this.defaultBlockState();
            blockState.setValue(ENABLE_AO, DEFAULT_ENABLE_AO);
            return blockPlaceContext.getClickedFace().getAxis() == Direction.Axis.Y
                    ? blockState.setValue(HORIZONTAL_FACING, blockPlaceContext.getHorizontalDirection())
                    : blockState.setValue(HORIZONTAL_FACING, blockPlaceContext.getClickedFace().getOpposite());
        } else {
            return super.getStateForPlacement(blockPlaceContext);
        }
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }
}