package com.yuushya.modelling.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.neoforge.client.anvilcraft.rendering.CacheableBERenderingPipeline;
import com.yuushya.modelling.neoforge.client.anvilcraft.rendering.CachedModeClient;
import com.yuushya.modelling.utils.CustomRenderInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/Camera;)V"
            )
    )
    void recompileBlockEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CacheableBERenderingPipeline.getInstance().runTasks();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBuffers;crumblingBufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
                    ordinal = 2
            )
    )
    void renderCachedBE(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        CacheableBERenderingPipeline.getInstance().render(frustumMatrix, projectionMatrix);
    }

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            )
    )
    <E extends BlockEntity> void wrapRenderBlockEntity(
            BlockEntityRenderDispatcher instance,
            E blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Operation<Void> original
    ) {
        if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
            CacheableBERenderingPipeline.getInstance().getRenderRegion(new ChunkPos(blockEntity.getBlockPos()))
                    .addIfPossible(blockEntity);
            return;
        }
        original.call(instance, blockEntity, partialTick, poseStack, bufferSource);
    }

    @Inject(at = @At("TAIL"), method = "renderLevel")
    void callRebuild(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CustomRenderInstance instance = CustomRenderInstance.getINSTANCE();
        if (level == null) return;
        if (instance.dirty) {
            Map<ChunkPos, HashSet<BlockPos>> cachedModeData = instance.getCachedModeData();
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
