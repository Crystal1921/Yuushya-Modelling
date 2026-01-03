package com.yuushya.modelling.utils;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomRenderInstance {
    @Getter
    private static final CustomRenderInstance INSTANCE = new CustomRenderInstance();
    @Getter
    private final Map<ChunkPos, Set<BlockPos>> cachedModeData;
    public volatile boolean dirty = false;

    private CustomRenderInstance() {
        cachedModeData = new ConcurrentHashMap<>();
    }

    /**
     * 线程安全地添加方块位置
     */
    public void addBlockPos(ChunkPos chunkPos, BlockPos blockPos) {
        cachedModeData.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(blockPos);
    }

    /**
     * 线程安全地批量添加方块位置
     */
    public void addAllBlockPos(ChunkPos chunkPos, Set<BlockPos> blockPosSet) {
        cachedModeData.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).addAll(blockPosSet);
    }

    /**
     * 线程安全地清除指定区块的数据
     */
    public void clearChunk(ChunkPos chunkPos) {
        cachedModeData.remove(chunkPos);
    }

    /**
     * 线程安全地清除所有数据
     */
    public void clearAll() {
        cachedModeData.clear();
    }
}
