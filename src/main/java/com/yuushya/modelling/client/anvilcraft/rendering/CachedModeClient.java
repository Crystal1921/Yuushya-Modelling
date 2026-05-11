package com.yuushya.modelling.client.anvilcraft.rendering;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_SPECIAL_RENDER;

public class CachedModeClient {
    public static final CachedModeClient INSTANCE = new CachedModeClient();

    public final Set<ChunkPos> safeSet = ConcurrentHashMap.newKeySet();

    public boolean isCachedModeEnabledOn(BlockEntity be) {
        if (be instanceof ItemBlockEntity itemBlockEntity) {
            return !itemBlockEntity.getBlockState().getValue(ENABLE_SPECIAL_RENDER);
        }
        return false;
    }
}
