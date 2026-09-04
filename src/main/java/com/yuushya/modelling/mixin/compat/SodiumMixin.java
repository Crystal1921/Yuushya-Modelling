package com.yuushya.modelling.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedBERenderingPipeline;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.SortedSet;

@Mixin(SodiumWorldRenderer.class)
public class SodiumMixin {

    /**
     * 等同于 LevelRendererMixin.wrapRenderBlockEntity，但作用于 SodiumWorldRenderer.extractBlockEntity。
     * Sodium 的 extractBlockEntity 调用 tryExtractRenderState(BlockEntity, float, CrumblingOverlay) 三参重载，
     * 此处拦截并将缓存模式的 BE 加入管线。
     */
    @WrapOperation(
            method = "extractBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"
            )
    )
    private <E extends BlockEntity, S extends BlockEntityRenderState> S wrapRenderBlockEntity(
            BlockEntityRenderDispatcher instance,
            E blockEntity,
            float partialTicks,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress,
            Operation<S> original) {
        if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
            CachedBERenderingPipeline.getInstance()
                    .getRenderRegion(ChunkPos.containing(blockEntity.getBlockPos()))
                    .addIfPossible(blockEntity);
            return null;
        }
        return original.call(instance, blockEntity, partialTicks, breakProgress);
    }

    @Inject(method = "extractBlockEntities",
            at = @At(
                    value = "TAIL"
            ))
    public void submitTask(Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, CallbackInfo ci) {
        Set<ChunkPos> safeSet = CachedModeClient.INSTANCE.safeSet;
        if (!safeSet.isEmpty()) {
            safeSet.forEach((chunkPos) -> {
                CachedBERenderingPipeline.getInstance().getRenderRegion(chunkPos).submitCompileTask();
            });
            safeSet.clear();
        }
    }
}
