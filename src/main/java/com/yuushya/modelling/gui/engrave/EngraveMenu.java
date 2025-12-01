//all right reserved

package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.gui.AbstractEngraveMenu;
import com.yuushya.modelling.registries.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

/**
 * Menu for the Engrave tool, allowing players to select from predefined engrave recipes.
 * Extends AbstractEngraveMenu and provides recipes from EngraveBlockResultLoader and EngraveItemResultLoader.
 */
public class EngraveMenu extends AbstractEngraveMenu {

    public EngraveMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EngraveMenu(int containerId, Inventory playerInventory, final ContainerLevelAccess access) {
        super(MenuRegistry.EngraveMenu.get(), containerId, playerInventory, access);
    }

    @Override
    protected List<EngraveBlockResult> getShowBlockRecipes() {
        return EngraveBlockResultLoader.SHOWBLOCK_ITEM_MAP.values().stream().toList();
    }

    @Override
    protected List<EngraveItemResult> getItemBlockRecipes() {
        return EngraveItemResultLoader.ITEMBLOCK_ITEM_MAP.values().stream().toList();
    }

    @Override
    public MenuType<?> getType() {
        return MenuRegistry.EngraveMenu.get();
    }
}

