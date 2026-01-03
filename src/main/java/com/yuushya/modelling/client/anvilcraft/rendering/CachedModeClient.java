package com.yuushya.modelling.client.anvilcraft.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CachedModeClient {
    public static final CachedModeClient INSTANCE = new CachedModeClient();
    private final Map<ChunkPos, Set<BlockPos>> cachedModeData = new ConcurrentHashMap<>();

    public boolean isCachedModeEnabledOn(BlockEntity be) {
        BlockEntityRenderer<?> renderer = Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .getRenderer(be);
        if (renderer == null) return false;
        return isCachedModeEnabledOn(be.getBlockPos());
    }

    public boolean isCachedModeEnabledOn(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<BlockPos> blockPosSet = cachedModeData.get(chunkPos);
        return blockPosSet != null && blockPosSet.contains(pos);
    }

    public void updateCachedModeData(ChunkPos chunkPos, List<BlockPos> blockPosList) {
        // 使用线程安全的 ConcurrentHashMap.newKeySet() 存储
        Set<BlockPos> blockPosSet = ConcurrentHashMap.newKeySet();
        blockPosSet.addAll(blockPosList);
        cachedModeData.put(chunkPos, blockPosSet);
        CacheableBERenderingPipeline.getInstance().updateFromNetwork(chunkPos, blockPosList);
    }

    /**
     * 线程安全地清除指定区块的缓存数据
     */
    public void clearChunk(ChunkPos chunkPos) {
        cachedModeData.remove(chunkPos);
    }

    /**
     * 线程安全地清除所有缓存数据
     */
    public void clearAll() {
        cachedModeData.clear();
    }
}
