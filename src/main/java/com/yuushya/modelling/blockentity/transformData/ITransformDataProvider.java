package com.yuushya.modelling.blockentity.transformData;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Common interface for objects that provide transform data information.
 * This interface allows for polymorphic access to position, rotation, and scale data
 * regardless of whether the underlying data is for blocks or items.
 * <p>
 * 建模界面只通过本接口读写元素，不关心背后是方块数据还是物品数据。
 */
public interface ITransformDataProvider {
    
    /**
     * Gets the position vector
     * @return position as Vector3d
     */
    org.joml.Vector3d getPosition();
    
    /**
     * Gets the rotation vector
     * @return rotation as Vector3f
     */
    org.joml.Vector3f getRotation();
    
    /**
     * Gets the scale vector
     * @return scale as Vector3f
     */
    org.joml.Vector3f getScale();
    
    /**
     * Gets whether this transform data is shown
     * @return true if shown, false otherwise
     */
    boolean isShown();
    
    /**
     * Sets whether this transform data is shown
     * @param shown true to show, false to hide
     */
    void setShown(boolean shown);

    void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries);

        // ---- 建模界面用的写入通道 ----

        void setPosition(double x, double y, double z);

        void setRotation(float x, float y, float z);

        void setScale(float x, float y, float z);

        /** 恢复为默认值（复用空槽位时用）。 */
        void reset();

        /** 用另一份数据整体覆盖自己；几何字段总是拷贝。 */
        void setFrom(ITransformDataProvider other);

        /** 列表行图标使用的物品形态；空元素返回 {@link ItemStack#EMPTY}。 */
        ItemStack iconStack();

        /** 空元素判定（AIR 方块 / 空物品栈）。 */
        boolean empty();
    }