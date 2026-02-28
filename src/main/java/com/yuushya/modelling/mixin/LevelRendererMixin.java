package com.yuushya.modelling.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.client.anvilcraft.rendering.CacheableBERenderingPipeline;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import com.yuushya.modelling.utils.CustomRenderInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;compileChunks(Lnet/minecraft/client/Camera;)V"
            )
    )
    void recompileBlockEntities(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        CacheableBERenderingPipeline.getInstance().runTasks();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V",
                    ordinal = 0
            )
    )
    void renderCachedBE(
            PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci
    ) {
        CacheableBERenderingPipeline.getInstance().render(pPoseStack.last().pose(), pProjectionMatrix);
    }

    // @WrapOperation(
    //         method = "renderLevel",
    //         at = @At(
    //                 value = "INVOKE",
    //                 target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
    //         )
    // )
    // <E extends BlockEntity> void wrapRenderBlockEntity(
    //         BlockEntityRenderDispatcher instance,
    //         E blockEntity,
    //         float partialTick,
    //         PoseStack poseStack,
    //         MultiBufferSource bufferSource,
    //         Operation<Void> original
    // ) {
    //     if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
    //         CacheableBERenderingPipeline.getInstance().getRenderRegion(new ChunkPos(blockEntity.getBlockPos()))
    //                 .addIfPossible(blockEntity);
    //         return;
    //     }
    //     original.call(instance, blockEntity, partialTick, poseStack, bufferSource);
    // }
    //
    // 已禁用：这个 mixin 每帧拦截所有方块实体渲染，开销巨大
    // 缓存更新已通过 callRebuild() 方法中的 CustomRenderInstance.dirty 标志统一处理

    @Inject(at = @At("TAIL"), method = "renderLevel")
    void callRebuild(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
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
