package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.AbstractYuushyaBlock;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

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
    public static final BooleanProperty FULL_BLOCK = BooleanProperty.create("full_block");
    public static final BooleanProperty ENABLE_SPECIAL_RENDER = BooleanProperty.create("enable_special_render");

    public static final Boolean DEFAULT_ENABLE_AO = false;
    public static final Boolean DEFAULT_FULL_BLOCK = false;
    public static final Boolean DEFAULT_ENABLE_SPECIAL_RENDER = false;

    public AbstractTransformBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        this.registerDefaultState(defaultBlockState()
                .setValue(POWERED, false)
                .setValue(LIT, 0)
                .setValue(HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(ENABLE_AO, DEFAULT_ENABLE_AO)
                .setValue(FULL_BLOCK, DEFAULT_FULL_BLOCK)
                .setValue(ENABLE_SPECIAL_RENDER, DEFAULT_ENABLE_SPECIAL_RENDER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(LIT).add(POWERED).add(HORIZONTAL_FACING).add(SHAPES).add(ENABLE_AO).add(FULL_BLOCK).add(ENABLE_SPECIAL_RENDER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        if (blockPlaceContext.getPlayer() != null && 
            blockPlaceContext.getPlayer().isHolding(ItemRegistry.ROT_TRANS_ITEM.get())) {
            BlockState blockState = this.defaultBlockState();
            blockState.setValue(ENABLE_AO, DEFAULT_ENABLE_AO);
            blockState.setValue(FULL_BLOCK, DEFAULT_FULL_BLOCK);
            blockState.setValue(ENABLE_SPECIAL_RENDER, DEFAULT_ENABLE_SPECIAL_RENDER);
            return blockPlaceContext.getClickedFace().getAxis() == Direction.Axis.Y
                    ? blockState.setValue(HORIZONTAL_FACING, blockPlaceContext.getHorizontalDirection())
                    : blockState.setValue(HORIZONTAL_FACING, blockPlaceContext.getClickedFace().getOpposite());
        } else {
            return super.getStateForPlacement(blockPlaceContext);
        }
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        BlockShape value = blockState.getValue(SHAPES);
        if (value != BlockShape.CUSTOM) {
            return value.voxelShape;
        }

        BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
        if (blockEntity instanceof AbstractTransformBlockEntity transformBlockEntity) {
            VoxelShape customShape = transformBlockEntity.getCustomShape();
            if (customShape != null) {
                return customShape;
            }
        }

        return super.getCollisionShape(blockState, blockGetter, blockPos, collisionContext);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack itemStack = new ItemStack(this);
        Map<String, String> properties = state.getProperties().stream().collect(Collectors.toMap((Property::getName), property -> YuushyaDebugStickItem.getNameHelper(state, property)));
        BlockItemStateProperties stateProperties = new BlockItemStateProperties(properties);
        itemStack.set(DataComponents.BLOCK_STATE, stateProperties);
        return itemStack;
    }
}