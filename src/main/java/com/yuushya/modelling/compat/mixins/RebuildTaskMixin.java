package com.yuushya.modelling.compat.mixins;

import com.yuushya.modelling.client.anvilcraft.rendering.CachedRegion;
import com.yuushya.modelling.compat.IrisSupport;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CachedRegion.RebuildTask.class)
public class RebuildTaskMixin {
    @Inject(
        method = "run",
        at = @At("HEAD")
    )
    void handleBegin(CallbackInfo ci) {
        IrisSupport.pushIrisGlobalState();
        ImmediateState.isRenderingLevel = true;
        ImmediateState.skipExtension.set(false);
    }

    @Inject(
        method = "run",
        at = @At("RETURN")
    )
    void handleEnd(CallbackInfo ci){
        IrisSupport.popIrisGlobalState();
    }

}
