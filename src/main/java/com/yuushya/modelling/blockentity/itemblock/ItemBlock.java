package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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

            if (context.isHoldingItem(ItemRegistry.GUI_ITEM.get())) {
                itemBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(ItemRegistry.ROT_TRANS_ITEM.get())) {
                itemBlockEntity.setShowRotAxis();
                itemBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.POS_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MICRO_POS_TRANS_ITEM.get())
            ) {
                itemBlockEntity.setShowPosAxis();
                itemBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.SLOT_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_SHOWBLOCK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_BLOCKSTATE_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.SCALE_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DEBUG_STICK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DESTROY_ITEM.get())
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
