package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public class ItemBlock extends AbstractTransformBlock {
    public ItemBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof ItemBlockEntity itemBlockEntity) {

            if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"gui_item")))){
                itemBlockEntity.setShowFrame();
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"rot_trans_item")))){
                itemBlockEntity.setShowRotAxis();
                itemBlockEntity.setShowText();
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"pos_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"micro_pos_trans_item")))
            ){
                itemBlockEntity.setShowPosAxis();
                itemBlockEntity.setShowText();
            }
            else if(context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"slot_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"get_showblock_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"move_transformdata_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"get_blockstate_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"scale_trans_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"debug_stick_item")))
                    ||context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"destroy_item")))
            ){
                itemBlockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(SHAPES).voxelShape;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ItemBlockEntity(blockPos, blockState);
    }
}
