package com.yuushya.modelling.utils;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CustomRenderInstance {
    @Getter
    private static final CustomRenderInstance INSTANCE = new CustomRenderInstance();
    @Getter
    private final Map<ChunkPos, HashSet<BlockPos>> cachedModeData;
    public boolean dirty = false;

    private CustomRenderInstance() {
        cachedModeData = new HashMap<>();
    }
}
