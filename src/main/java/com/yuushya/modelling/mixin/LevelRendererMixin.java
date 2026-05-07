package com.yuushya.modelling.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.client.anvilcraft.rendering.CacheableBERenderingPipeline;
import com.yuushya.modelling.utils.CustomRenderInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(
            method = "compileSections",
            at = @At("TAIL")
    )
    void recompileBlockEntities(Camera camera, CallbackInfo ci) {
        CacheableBERenderingPipeline.getInstance().runTasks();
    }

    @Inject(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderTranslucentFeatures()V",
                    shift = At.Shift.AFTER
            )
    )
    void renderCachedBE(GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, Matrix4fc modelViewMatrix, ResourceHandle entityOutlineTarget, ResourceHandle translucentTarget, ResourceHandle mainTarget, ResourceHandle itemEntityTarget, ResourceHandle particleTarget, boolean renderOutline, CallbackInfo ci) {
        CacheableBERenderingPipeline.getInstance().render();
    }

    @Inject(at = @At("TAIL"), method = "renderLevel")
    void callRebuild(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CustomRenderInstance instance = CustomRenderInstance.getINSTANCE();
        if (level == null) return;
        if (instance.dirty) {
            Map<ChunkPos, Set<BlockPos>> cachedModeData = instance.getCachedModeData();
            cachedModeData.forEach((chunkPos, blockPosSet) -> {
                if (blockPosSet.isEmpty()) return;
                List<BlockPos> list = blockPosSet.stream()
                        .filter(blockPos -> level.getBlockEntity(blockPos) instanceof ItemBlockEntity)
                        .toList();
                CachedModeClient.INSTANCE.updateCachedModeData(chunkPos, list);
            });
            instance.dirty = false;
        }
    }
}
