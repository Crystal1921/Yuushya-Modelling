package com.yuushya.modelling.blockentity.transformData;

import net.minecraft.nbt.CompoundTag;

/**
 * Common interface for objects that provide transform data information.
 * This interface allows for polymorphic access to position, rotation, and scale data
 * regardless of whether the underlying data is for blocks or items.
 */
public interface ITransformDataProvider {

    /**
     * Gets the position vector
     *
     * @return position as Vector3d
     */
    org.joml.Vector3d getPosition();

    /**
     * Gets the rotation vector
     *
     * @return rotation as Vector3f
     */
    org.joml.Vector3f getRotation();

    /**
     * Gets the scale vector
     *
     * @return scale as Vector3f
     */
    org.joml.Vector3f getScale();

    /**
     * Gets whether this transform data is shown
     *
     * @return true if shown, false otherwise
     */
    boolean isShown();

    /**
     * Sets whether this transform data is shown
     *
     * @param shown true to show, false to hide
     */
    void setShown(boolean shown);

    void saveAdditional(CompoundTag compoundTag);
}