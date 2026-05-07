package com.yuushya.modelling.mixin.compat;

import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EmbeddiumWorldRenderer.class)
public class EmbeddiumWorldRenderMixin {
    // @WrapOperation(
    //     method = "renderBlockEntity",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
    //     )
    // )
    // private static <E extends BlockEntity> void wrapRenderBlockEntity(
    //     BlockEntityRenderDispatcher instance,
    //     E blockEntity,
    //     float partialTick,
    //     PoseStack poseStack,
    //     MultiBufferSource bufferSource,
    //     Operation<Void> original
    // ) {
    //     if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
    //         CacheableBERenderingPipeline.getInstance().getRenderRegion(new ChunkPos(blockEntity.getBlockPos()))
    //             .addIfPossible(blockEntity);
    //         return;
    //     }
    //     original.call(instance, blockEntity, partialTick, poseStack, bufferSource);
    // }
    //
    // 已禁用：这个 mixin 每帧拦截所有方块实体渲染，开销巨大
    // 缓存更新已通过 LevelRendererMixin.callRebuild() 方法统一处理
}
