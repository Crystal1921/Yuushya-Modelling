//all right reserved

package com.yuushya.modelling.gui.history;

import com.yuushya.modelling.gui.AbstractEngraveMenu;
import com.yuushya.modelling.gui.engrave.IEngraveResult;
import com.yuushya.modelling.item.showblocktool.HistoryItem;
import com.yuushya.modelling.registries.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Menu for the History tool, allowing players to select from previously saved history recipes.
 * Extends AbstractEngraveMenu and provides recipes from HistoryItem's history maps.
 */
public class HistoryMenu extends AbstractEngraveMenu {

    public HistoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public HistoryMenu(int containerId, Inventory playerInventory, final ContainerLevelAccess access) {
        super(MenuRegistry.HistoryMenu.get(), containerId, playerInventory, access);
    }

    @Override
    protected List<IEngraveResult> getShowBlockRecipes() {
        return HistoryItem.HISTORY_SHOWBLOCK_MAP.values().stream().toList();
    }

    @Override
    protected List<IEngraveResult> getItemBlockRecipes() {
        return HistoryItem.HISTORY_ITEMBLOCK_ITEM_MAP.values().stream().toList();
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return MenuRegistry.HistoryMenu.get();
    }
}

