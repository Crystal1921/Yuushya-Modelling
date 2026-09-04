package com.yuushya.modelling.mixin;

import com.yuushya.modelling.client.anvilcraft.rendering.CachedBlockEntityRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(
            method = "updateLevelInEngines*",
            at = @At("HEAD")
    )
    void updateLevel(ClientLevel level, CallbackInfo ci) {
        CachedBlockEntityRenderingPipeline.updateLevel(level);
    }

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onCreateInstance(GameConfig gameConfig, CallbackInfo ci) {
        CachedBlockEntityRenderingPipeline.create();
    }
}
