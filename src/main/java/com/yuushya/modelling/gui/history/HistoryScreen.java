//all right reserved

package com.yuushya.modelling.gui.history;

import com.yuushya.modelling.gui.AbstractEngraveScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the History tool, allowing players to view and select from previously saved history recipes.
 * Extends AbstractEngraveScreen to inherit common rendering and interaction logic.
 */
public class HistoryScreen extends AbstractEngraveScreen<HistoryMenu> {

    public HistoryScreen(HistoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}

