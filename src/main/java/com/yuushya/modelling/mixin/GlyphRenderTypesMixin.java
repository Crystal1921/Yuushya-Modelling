package com.yuushya.modelling.mixin;

import com.yuushya.modelling.mixinInterface.GlyphRenderTypesExt;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderTypes.class)
public class GlyphRenderTypesMixin implements GlyphRenderTypesExt {
    @Unique
    private ResourceLocation copilot$id;

    @Override
    public ResourceLocation yuushya_Modelling$getId() {
        return copilot$id;
    }

    @Override
    public void copilot$setId(ResourceLocation id) {
        this.copilot$id = id;
    }

    // 对 createForIntensityTexture 注入
    @Inject(method = "createForIntensityTexture", at = @At("RETURN"))
    private static void copilot$storeIdIntensity(ResourceLocation id, CallbackInfoReturnable<GlyphRenderTypes> cir) {
        GlyphRenderTypes grt = cir.getReturnValue();
        ((GlyphRenderTypesExt) (Object) grt).copilot$setId(id);
    }

    // 对 createForColorTexture 注入
    @Inject(method = "createForColorTexture", at = @At("RETURN"))
    private static void copilot$storeIdColor(ResourceLocation id, CallbackInfoReturnable<GlyphRenderTypes> cir) {
        GlyphRenderTypes grt = cir.getReturnValue();
        ((GlyphRenderTypesExt) (Object) grt).copilot$setId(id);
    }
}