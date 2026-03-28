package com.yuushya.modelling.utils;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Utility class for serializing and deserializing VoxelShape to/from NBT format.
 * Uses box-based representation for efficient storage and reconstruction.
 */
public class VoxelShapeSerializer {

    private static final String BOXES_KEY = "Boxes";
    private static final String SHAPE_TYPE_KEY = "ShapeType";

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
        List<Double> boxData = Lists.newArrayList();

        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            CompoundTag boxTag = new CompoundTag();
            boxTag.putDouble("x1", x1);
            boxTag.putDouble("y1", y1);
            boxTag.putDouble("z1", z1);
            boxTag.putDouble("x2", x2);
            boxTag.putDouble("y2", y2);
            boxTag.putDouble("z2", z2);
            boxesList.add(boxTag);
        });

        tag.put(BOXES_KEY, boxesList);
        return tag;
    }

    /**
     * Deserializes a VoxelShape from NBT format.
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

        return switch (shapeType) {
            case EMPTY -> Shapes.empty();
            case BLOCK -> Shapes.block();
            case BOXES -> deserializeBoxes(tag);
        };
    }

    /**
     * Deserializes boxes from NBT and reconstructs the VoxelShape.
     */
    private static VoxelShape deserializeBoxes(CompoundTag tag) {
        if (!tag.contains(BOXES_KEY, Tag.TAG_LIST)) {
            return Shapes.empty();
        }

        ListTag boxesList = tag.getList(BOXES_KEY, Tag.TAG_COMPOUND);
        if (boxesList.isEmpty()) {
            return Shapes.empty();
        }

        // Start with empty shape and join all boxes
        VoxelShape result = Shapes.empty();

        for (Tag boxTag : boxesList) {
            CompoundTag box = (CompoundTag) boxTag;
            double x1 = box.getDouble("x1");
            double y1 = box.getDouble("y1");
            double z1 = box.getDouble("z1");
            double x2 = box.getDouble("x2");
            double y2 = box.getDouble("y2");
            double z2 = box.getDouble("z2");

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
            return tag.getList(BOXES_KEY, Tag.TAG_COMPOUND).size();
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
}
