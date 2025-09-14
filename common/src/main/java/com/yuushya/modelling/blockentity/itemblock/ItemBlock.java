package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ItemBlock extends AbstractTransformBlock {
    public ItemBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ItemBlockEntity(blockPos, blockState);
    }
}
