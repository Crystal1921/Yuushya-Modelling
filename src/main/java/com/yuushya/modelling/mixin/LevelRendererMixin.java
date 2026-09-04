package com.yuushya.modelling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedBlockEntityRenderingPipeline;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

//    @Inject(
//            method = "compileSections",
//            at = @At("TAIL")
//    )
//    void recompileBlockEntities(Camera camera, CallbackInfo ci) {
//        CachedBlockEntityRenderingPipeline.getInstance().runTasks();
//    }

//    @Inject(
//            method = "lambda$addMainPass$0",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderTranslucentFeatures()V",
//                    shift = At.Shift.AFTER
//            )
//    )
//    void renderCachedBE(GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, Matrix4fc modelViewMatrix, ResourceHandle entityOutlineTarget, ResourceHandle translucentTarget, ResourceHandle mainTarget, ResourceHandle itemEntityTarget, ResourceHandle particleTarget, boolean renderOutline, CallbackInfo ci) {
//        CacheableBERenderingPipeline.getInstance().render();
//    }

    @WrapOperation(
            method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"
            )
    )
    <E extends BlockEntity, S extends BlockEntityRenderState> S wrapRenderBlockEntity(BlockEntityRenderDispatcher instance, E blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, @Nullable Frustum frustum, Operation<S> original) {
        if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
            CachedBlockEntityRenderingPipeline.getInstance().getRenderRegion(ChunkPos.containing(blockEntity.getBlockPos()))
                    .addIfPossible(blockEntity);
            return null;
        }

        return original.call(instance, blockEntity, partialTicks, breakProgress, frustum);
    }

    @Inject(method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(
                    value = "TAIL"
            ))
    public void submitTask(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState, Frustum frustum, CallbackInfo ci) {
        Set<ChunkPos> safeSet = CachedModeClient.INSTANCE.safeSet;
        if (!safeSet.isEmpty()) {
            safeSet.forEach((chunkPos) -> {
                CachedBlockEntityRenderingPipeline.getInstance().getRenderRegion(chunkPos).submitCompileTask();
            });
            safeSet.clear();
        }
    }
}
