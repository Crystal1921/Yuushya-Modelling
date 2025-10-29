package com.yuushya.modelling.fabric.mixin;

import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourMixin {
    @Inject(at = @At(value = "HEAD"), method = "isCollisionShapeFullBlock", cancellable = true)
    private void isCollisionShapeFullBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = ((BlockBehaviour.BlockStateBase)(Object)this).asState();
        if(state.is(YuushyaRegistries.BLOCKS.get("itemblock").get())) {
            boolean isFull = state.getValue(ENABLE_AO);
            cir.setReturnValue(isFull);
        }
    }

}
