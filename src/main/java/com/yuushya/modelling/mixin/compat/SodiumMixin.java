package com.yuushya.modelling.mixin.compat;

import com.yuushya.modelling.client.anvilcraft.rendering.CacheableBERenderingPipeline;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

@Mixin(SodiumWorldRenderer.class)
public class SodiumMixin {
    @Inject(method = "extractBlockEntities",
            at = @At(
                    value = "TAIL"
            ))
    public void submitTask(Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, CallbackInfo ci) {
        Set<ChunkPos> safeSet = CachedModeClient.INSTANCE.safeSet;
        if (!safeSet.isEmpty()) {
            safeSet.forEach((chunkPos) -> {
                CacheableBERenderingPipeline.getInstance().getRenderRegion(chunkPos).submitCompileTask();
            });
            safeSet.clear();
        }
    }
}
