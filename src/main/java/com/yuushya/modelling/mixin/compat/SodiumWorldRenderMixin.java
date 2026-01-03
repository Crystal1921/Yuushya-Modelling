package com.yuushya.modelling.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.client.anvilcraft.rendering.CacheableBERenderingPipeline;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRenderMixin {
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
