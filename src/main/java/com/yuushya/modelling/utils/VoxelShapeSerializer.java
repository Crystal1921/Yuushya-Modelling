package com.yuushya.modelling.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Utility class for serializing and deserializing VoxelShape to/from NBT format.
 * Uses box-based representation for efficient storage and reconstruction.
 */
public class VoxelShapeSerializer {

    private static final String BOXES_KEY = "Boxes";

    /**
     * Cache for deserialized VoxelShapes to avoid reconstructing identical shapes.
     * Uses Guava Cache with weak references and automatic eviction.
     */
    private static final Cache<CompoundTag, VoxelShape> SHAPE_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .maximumSize(500)
            .build();

    /**
     * Serializes a VoxelShape to NBT format using box representation.
     *
     * @param shape The VoxelShape to serialize
     * @return CompoundTag containing the serialized shape data
     */
    public static CompoundTag serializeVoxelShape(VoxelShape shape) {
        CompoundTag tag = new CompoundTag();
        ListTag boxesList = new ListTag();

        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            ListTag box = new ListTag();
            box.add(DoubleTag.valueOf(x1));
            box.add(DoubleTag.valueOf(y1));
            box.add(DoubleTag.valueOf(z1));
            box.add(DoubleTag.valueOf(x2));
            box.add(DoubleTag.valueOf(y2));
            box.add(DoubleTag.valueOf(z2));
            boxesList.add(box);
        });

        tag.put(BOXES_KEY, boxesList);
        return tag;
    }

    /**
     * Deserializes a VoxelShape from NBT format with caching support.
     *
     * @param tag The CompoundTag containing the serialized shape data
     * @return The reconstructed VoxelShape
     */
    public static VoxelShape deserializeVoxelShape(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return Shapes.empty();
        }

        // Use cache to avoid expensive reconstruction
        try {
            return SHAPE_CACHE.get(tag, () -> deserializeBoxes(tag));
        } catch (ExecutionException e) {
            // Fallback to direct deserialization if caching fails
            return deserializeBoxes(tag);
        }
    }

    /**
     * Deserializes boxes from NBT and reconstructs the VoxelShape.
     */
    private static VoxelShape deserializeBoxes(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return Shapes.empty();
        }

        ListTag boxesList = tag.getList(BOXES_KEY, Tag.TAG_LIST);
        if (boxesList.isEmpty()) {
            return Shapes.empty();
        }

        // Start with empty shape and join all boxes
        VoxelShape result = Shapes.empty();

        for (Tag boxTag : boxesList) {
            ListTag box = (ListTag) boxTag;
            double x1 = box.getDouble(0);
            double y1 = box.getDouble(1);
            double z1 = box.getDouble(2);
            double x2 = box.getDouble(3);
            double y2 = box.getDouble(4);
            double z2 = box.getDouble(5);

            VoxelShape boxShape = Shapes.box(x1, y1, z1, x2, y2, z2);
            result = Shapes.joinUnoptimized(result, boxShape, BooleanOp.OR);
        }

        // Optimize the final shape
        return result.optimize();
    }

    /**
     * Utility method to get box count from serialized shape.
     *
     * @param tag The serialized shape tag
     * @return Number of boxes in the shape
     */
    public static int getBoxCount(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return 0;
        }
        return tag.getList(BOXES_KEY, Tag.TAG_LIST).size();
    }

    /**
     * Checks if the serialized shape represents an empty shape.
     *
     * @param tag The serialized shape tag
     * @return true if the shape is empty
     */
    public static boolean isEmpty(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return true;
        }
        return tag.getList(BOXES_KEY, Tag.TAG_LIST).isEmpty();
    }

    /**
     * Clears the shape cache. Useful for testing or memory management.
     */
    public static void clearCache() {
        SHAPE_CACHE.invalidateAll();
    }

    /**
     * Returns the current cache size for monitoring/debugging.
     *
     * @return Number of cached shapes
     */
    public static long getCacheSize() {
        return SHAPE_CACHE.size();
    }
}
