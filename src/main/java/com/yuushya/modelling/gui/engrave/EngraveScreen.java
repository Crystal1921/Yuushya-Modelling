//all right reserved

package com.yuushya.modelling.gui.engrave;

import com.yuushya.modelling.gui.AbstractEngraveScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Engrave tool, allowing players to view and select from predefined engrave recipes.
 * Extends AbstractEngraveScreen to inherit common rendering and interaction logic.
 */
public class EngraveScreen extends AbstractEngraveScreen<EngraveMenu> {

    public EngraveScreen(EngraveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}

