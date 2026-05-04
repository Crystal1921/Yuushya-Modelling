package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.LIT;
import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public class ItemBlock extends AbstractTransformBlock {
    public ItemBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof ItemBlockEntity itemBlockEntity) {

            if (context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "gui_item")))) {
                itemBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "rot_trans_item")))) {
                itemBlockEntity.setShowRotAxis();
                itemBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "pos_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "micro_pos_trans_item")))
            ) {
                itemBlockEntity.setShowPosAxis();
                itemBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "slot_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "get_showblock_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "move_transformdata_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "get_blockstate_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "scale_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "debug_stick_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "destroy_item")))
            ) {
                itemBlockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ItemBlockEntity(blockPos, blockState);
    }

    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        return state;
    }
}
