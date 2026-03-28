package com.yuushya.modelling.utils;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for serializing and deserializing VoxelShape to/from NBT format.
 * Uses box-based representation for efficient storage and reconstruction.
 */
public class VoxelShapeSerializer {

    private static final String BOXES_KEY = "Boxes";
    private static final String SHAPE_TYPE_KEY = "ShapeType";

    /**
     * Cache for deserialized VoxelShapes to avoid reconstructing identical shapes.
     * Uses CompoundTag as key (relies on its equals/hashCode implementation).
     */
    private static final Map<CompoundTag, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

    /**
     * Shape types for optimized deserialization
     */
    private enum ShapeType {
        EMPTY,
        BLOCK,
        BOXES
    }

    /**
     * Serializes a VoxelShape to NBT format using box representation.
     *
     * @param shape The VoxelShape to serialize
     * @return CompoundTag containing the serialized shape data
     */
    public static CompoundTag serializeVoxelShape(VoxelShape shape) {
        CompoundTag tag = new CompoundTag();

        if (shape.isEmpty()) {
            tag.putString(SHAPE_TYPE_KEY, ShapeType.EMPTY.name());
            return tag;
        }

        // Check if it's a full block (optimized case)
        AABB bounds = shape.bounds();
        if (isFullBlock(shape, bounds)) {
            tag.putString(SHAPE_TYPE_KEY, ShapeType.BLOCK.name());
            return tag;
        }

        // Serialize as collection of boxes
        tag.putString(SHAPE_TYPE_KEY, ShapeType.BOXES.name());

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
        if (!tag.contains(SHAPE_TYPE_KEY)) {
            return Shapes.empty();
        }

        String shapeTypeName = tag.getString(SHAPE_TYPE_KEY);
        ShapeType shapeType;

        try {
            shapeType = ShapeType.valueOf(shapeTypeName);
        } catch (IllegalArgumentException e) {
            // Legacy format fallback or invalid type
            return deserializeLegacyFormat(tag);
        }

        // Simple cases don't need caching (they return singletons)
        if (shapeType == ShapeType.EMPTY) {
            return Shapes.empty();
        }
        if (shapeType == ShapeType.BLOCK) {
            return Shapes.block();
        }

        // For BOXES type, use cache to avoid expensive reconstruction
        return SHAPE_CACHE.computeIfAbsent(tag, VoxelShapeSerializer::deserializeBoxesUncached);
    }

    /**
     * Internal method that performs actual deserialization without caching.
     * Used by the cache loader.
     */
    private static VoxelShape deserializeBoxesUncached(CompoundTag tag) {
        return deserializeBoxes(tag);
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
     * Checks if the shape is a full block (1x1x1 cube from 0,0,0 to 1,1,1).
     */
    private static boolean isFullBlock(VoxelShape shape, AABB bounds) {
        if (bounds.minX != 0.0 || bounds.minY != 0.0 || bounds.minZ != 0.0) {
            return false;
        }
        if (bounds.maxX != 1.0 || bounds.maxY != 1.0 || bounds.maxZ != 1.0) {
            return false;
        }

        // Check if shape matches block shape
        return shape == Shapes.block() || shape.equals(Shapes.block());
    }

    /**
     * Fallback for legacy format or unknown format.
     * Attempts to read as list of AABB boxes.
     */
    private static VoxelShape deserializeLegacyFormat(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return Shapes.empty();
        }

        // Try to deserialize using the boxes list
        return deserializeBoxes(tag);
    }

    /**
     * Utility method to get box count from serialized shape.
     *
     * @param tag The serialized shape tag
     * @return Number of boxes in the shape, or 0 if empty/single block
     */
    public static int getBoxCount(CompoundTag tag) {
        if (!tag.contains(SHAPE_TYPE_KEY)) {
            return 0;
        }

        String shapeTypeName = tag.getString(SHAPE_TYPE_KEY);
        ShapeType shapeType;

        try {
            shapeType = ShapeType.valueOf(shapeTypeName);
        } catch (IllegalArgumentException e) {
            return 0;
        }

        if (shapeType == ShapeType.BOXES && tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return tag.getList(BOXES_KEY, Tag.TAG_LIST).size();
        }

        return shapeType == ShapeType.BLOCK ? 1 : 0;
    }

    /**
     * Checks if the serialized shape represents an empty shape.
     *
     * @param tag The serialized shape tag
     * @return true if the shape is empty
     */
    public static boolean isEmpty(CompoundTag tag) {
        if (!tag.contains(SHAPE_TYPE_KEY)) {
            return true;
        }

        String shapeTypeName = tag.getString(SHAPE_TYPE_KEY);
        return ShapeType.EMPTY.name().equals(shapeTypeName);
    }

    /**
     * Clears the shape cache. Useful for testing or memory management.
     */
    public static void clearCache() {
        SHAPE_CACHE.clear();
    }

    /**
     * Returns the current cache size for monitoring/debugging.
     *
     * @return Number of cached shapes
     */
    public static int getCacheSize() {
        return SHAPE_CACHE.size();
    }
}
