package com.yuushya.modelling.compat.mixins;

import com.yuushya.modelling.client.anvilcraft.rendering.CachedRegion;
import com.yuushya.modelling.compat.IrisSupport;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(CachedRegion.class)
public class CachedChunkMixin {
    @Inject(
        method = "modifyRenderTypeIfNeeded",
        at = @At("HEAD"),
        cancellable = true
    )
    void unwrapIrisRenderType(RenderType rt, CallbackInfoReturnable<RenderType> cir) {
        cir.setReturnValue(IrisSupport.unwrapRenderType(rt));
    }
}
