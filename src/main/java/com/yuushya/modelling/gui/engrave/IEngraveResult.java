package com.yuushya.modelling.gui.engrave;

import net.minecraft.world.item.ItemStack;

/**
 * Common interface for engrave results.
 * This allows EngraveMenu to work with both block and item results.
 */
public interface IEngraveResult {
    /**
     * Gets the name of this engrave result
     * @return the name
     */
    String getName();

    /**
     * Gets the result item stack
     * @return the result item stack
     */
    ItemStack getResultItem();
}
