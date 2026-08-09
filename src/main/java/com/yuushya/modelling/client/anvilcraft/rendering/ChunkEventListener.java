package com.yuushya.modelling.client.anvilcraft.rendering;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * 监听客户端区块加载/卸载，主动把缓存模式的 ItemBlock 加入/移出缓存管线。
 *
 * <p>背景：ItemBlock 默认通过每帧 extract（LevelRenderer / SodiumWorldRenderer 的
 * extractBlockEntity）→ {@link CachedRegion#addIfPossible} 进入缓存管线，但这依赖
 * ItemBlock 出现在 Sodium 区块段的 BE 列表里（由区块段网格编译时烘入）。区块卸载后
 * 重新加载时，若新区块段的网格编译早于区块实体的注册，ItemBlock 不会被烘入 BE 列表，
 * 从而永远不会被重新 addIfPossible，导致不渲染。</p>
 *
 * <p>这里在客户端 {@link ChunkEvent.Load}（此时区块实体已注册完毕）主动扫描并加入缓存，
 * 在 {@link ChunkEvent.Unload} 时清理，作为每帧 extract 路径的兜底。</p>
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ChunkEventListener {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide()) return;
        LevelChunk chunk = event.getChunk();
        CacheableBERenderingPipeline pipeline = CacheableBERenderingPipeline.getInstance();
        if (pipeline == null) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
                pipeline.getRenderRegion(ChunkPos.containing(blockEntity.getBlockPos()))
                        .addIfPossible(blockEntity);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        LevelChunk chunk = event.getChunk();
        CacheableBERenderingPipeline pipeline = CacheableBERenderingPipeline.getInstance();
        if (pipeline == null) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (CachedModeClient.INSTANCE.isCachedModeEnabledOn(blockEntity)) {
                pipeline.getRenderRegion(ChunkPos.containing(blockEntity.getBlockPos()))
                        .blockRemoved(blockEntity);
            }
        }
    }
}
