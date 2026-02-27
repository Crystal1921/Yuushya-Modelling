package com.yuushya.modelling.mixin;

import com.yuushya.modelling.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.FULL_BLOCK;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourMixin {
    @Inject(at = @At(value = "HEAD"), method = "isCollisionShapeFullBlock", cancellable = true)
    private void isCollisionShapeFullBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = ((BlockState) (Object) this);
        if (state.is(BlockRegistry.ITEM_BLOCK.get())) {
            boolean isFull = state.getValue(FULL_BLOCK);
            cir.setReturnValue(isFull);
        }
    }

}
