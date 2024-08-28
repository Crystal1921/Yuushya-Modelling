package com.yuushya.modelling.blockentity.showblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.AbstractYuushyaBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

public class ShowBlock extends AbstractYuushyaBlock implements EntityBlock {
    public ShowBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        this.registerDefaultState(defaultBlockState().setValue(POWERED,false).setValue(LIT,0).setValue(HORIZONTAL_FACING,Direction.SOUTH));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock())){
            if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"rot_trans_item")))){
                ShowBlockEntity blockEntity = (ShowBlockEntity) level.getBlockEntity(pos);
                if(blockEntity!=null) {
                    blockEntity.setShowFrame();
                }
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"rot_trans_item")))){
                ShowBlockEntity blockEntity = (ShowBlockEntity) level.getBlockEntity(pos);
                if(blockEntity!=null) {
                    blockEntity.setShowRotAixs();
                    blockEntity.setShowText();
                }
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"pos_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"micro_pos_trans_item")))
            ){
                ShowBlockEntity blockEntity = (ShowBlockEntity) level.getBlockEntity(pos);
                if(blockEntity!=null) {
                    blockEntity.setShowPosAixs();
                    blockEntity.setShowText();
                }
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"slot_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"get_showblock_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"move_transformdata_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"get_blockstate_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"scale_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"debug_stick_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"destroy_item")))
            ){
                ShowBlockEntity blockEntity = (ShowBlockEntity) level.getBlockEntity(pos);
                if(blockEntity!=null) blockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ShowBlockEntity(blockPos,blockState);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(LIT).add(POWERED).add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        if(blockPlaceContext.getPlayer().isSecondaryUseActive())
            return blockPlaceContext.getClickedFace().getAxis() == Direction.Axis.Y
                ? this.defaultBlockState().setValue(HORIZONTAL_FACING, blockPlaceContext.getHorizontalDirection())
                : this.defaultBlockState().setValue(HORIZONTAL_FACING, blockPlaceContext.getClickedFace().getOpposite());
        else
            return super.getStateForPlacement(blockPlaceContext);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos){
        ShowBlockEntity showBlockEntity= (ShowBlockEntity) worldIn.getBlockEntity(currentPos);
        BlockState blockState=showBlockEntity.getTransformData(0).blockState;
        Block block=blockState.getBlock();

        if (facingState.getBlock() instanceof ShowBlock){
            showBlockEntity.saveChanged();
            return stateIn;
            //facingState= YuushyaUtils.getBlockState(facingState,worldIn,facingPos);
        }
        if (!(block instanceof AirBlock)){
            showBlockEntity.getTransformData(0).blockState=blockState.updateShape(facing,facingState,worldIn,currentPos,facingPos);
            showBlockEntity.saveChanged();
            return stateIn.setValue(POWERED,!stateIn.getValue(POWERED));
        }
        showBlockEntity.saveChanged();
        return stateIn.setValue(POWERED,!stateIn.getValue(POWERED));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }
}
