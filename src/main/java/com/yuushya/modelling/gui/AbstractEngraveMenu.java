//all right reserved

package com.yuushya.modelling.gui;

import com.google.common.collect.Lists;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.textblock.TextBlock;
import com.yuushya.modelling.gui.engrave.EngraveBlockResult;
import com.yuushya.modelling.gui.engrave.EngraveItemResult;
import com.yuushya.modelling.gui.engrave.EngraveTextResult;
import com.yuushya.modelling.gui.engrave.IEngraveResult;
import com.yuushya.modelling.network.TransformDataListPacket;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for Engrave-style menus (EngraveMenu and HistoryMenu).
 * This class contains all the common logic for recipe selection and result handling.
 */
public abstract class AbstractEngraveMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INV_SLOT_START = 2;
    private static final int INV_SLOT_END = 29;
    private static final int USE_ROW_SLOT_START = 29;
    private static final int USE_ROW_SLOT_END = 38;
    
    final Slot inputSlot;
    /**
     * The inventory slot that stores the output of the crafting recipe.
     */
    final Slot resultSlot;
    /**
     * The inventory that stores the output of the crafting recipe.
     */
    final ResultContainer resultContainer = new ResultContainer();
    private final ContainerLevelAccess access;
    /**
     * The index of the selected recipe in the GUI.
     */
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final Level level;
    /**
     * Stores the game time of the last time the player took items from the the crafting result slot. 
     * This is used to prevent the sound from being played multiple times on the same tick.
     */
    long lastSoundTime;
    Runnable slotUpdateListener = () -> {
    };
    @Getter
    private List<IEngraveResult> recipes = Lists.newArrayList();
    /**
     * The {@linkplain ItemStack} set in the input slot by the player.
     */
    private ItemStack input = ItemStack.EMPTY;
    /**
     * Determines whether to use block or item recipes based on the input item.
     */
    private BLOCK_TYPE useItemRecipes = BLOCK_TYPE.BLOCK;
    
    public final Container container = new SimpleContainer(1) {
        public void setChanged() {
            super.setChanged();
            AbstractEngraveMenu.this.slotsChanged(this);
            AbstractEngraveMenu.this.slotUpdateListener.run();
        }
    };

    /**
     * Constructor for client-side menu creation from network data.
     * The extraData parameter is provided by the IMenuTypeExtension interface for network-based menu creation,
     * but is not used in this implementation as all data is derived from the player's inventory and level.
     * 
     * @param menuType The menu type
     * @param containerId The container ID
     * @param playerInventory The player's inventory
     * @param extraData Extra data from network buffer (unused, required by IMenuTypeExtension interface)
     */
    public AbstractEngraveMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public AbstractEngraveMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, final ContainerLevelAccess access) {
        super(menuType, containerId);
        int i;
        this.access = access;
        this.level = playerInventory.player.level();
        this.inputSlot = this.addSlot(new Slot(this.container, INPUT_SLOT, 20, 33));
        this.resultSlot = this.addSlot(new Slot(this.resultContainer, RESULT_SLOT, 143, 33) {

            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ItemStack itemStack = AbstractEngraveMenu.this.inputSlot.remove(1);
                if (!itemStack.isEmpty()) {
                    AbstractEngraveMenu.this.setupResultSlotServer(stack);
                }
                access.execute((level, blockPos) -> {
                    long l = level.getGameTime();
                    if (AbstractEngraveMenu.this.lastSoundTime != l) {
                        level.playSound(null, blockPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        AbstractEngraveMenu.this.lastSoundTime = l;
                    }
                });
                super.onTake(player, stack);
            }

            private List<ItemStack> getRelevantItems() {
                return List.of(AbstractEngraveMenu.this.inputSlot.getItem());
            }
        });
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.addDataSlot(this.selectedRecipeIndex);
    }

    /**
     * Returns the index of the selected recipe.
     */
    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex.get();
    }

    public int getNumRecipes() {
        return this.recipes.size();
    }

    public boolean hasInputItem() {
        return this.inputSlot.hasItem() && !this.recipes.isEmpty();
    }

    public boolean stillValid(@NotNull Player player) {
        return !container.getItem(INPUT_SLOT).isEmpty();
    }

    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (this.isValidRecipeIndex(id)) {
            this.selectedRecipeIndex.set(id);
            this.setupResultSlot();
        }
        return true;
    }

    private boolean isValidRecipeIndex(int recipeIndex) {
        return recipeIndex >= 0 && recipeIndex < this.recipes.size();
    }

    public void slotsChanged(@NotNull Container container) {
        ItemStack itemStack = this.inputSlot.getItem();
        if (!itemStack.is(this.input.getItem())) {
            this.input = itemStack.copy();
            this.setupRecipeList(container, itemStack);
        }
    }

    private void setupRecipeList(Container container, ItemStack stack) {
        this.recipes.clear();
        this.selectedRecipeIndex.set(-1);
        this.resultSlot.set(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            // Check if the input item is an itemblock or showblock
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof ItemBlock) {
                    this.useItemRecipes = BLOCK_TYPE.BLOCK;
                    this.recipes = new ArrayList<>(getItemBlockRecipes());
                } else if (blockItem.getBlock() instanceof TextBlock) {
                    this.useItemRecipes = BLOCK_TYPE.TEXT;
                    this.recipes = new ArrayList<>(getTextBlockRecipes());
                } else {
                    this.useItemRecipes = BLOCK_TYPE.ITEM;
                    this.recipes = new ArrayList<>(getShowBlockRecipes());
                }
            }
        }
    }

    /**
     * Gets the list of recipes for ShowBlock items.
     * Subclasses must implement this to provide their specific recipe source.
     *
     * @return List of engrave results for ShowBlock
     */
    protected abstract List<EngraveBlockResult> getShowBlockRecipes();

    /**
     * Gets the list of recipes for ItemBlock items.
     * Subclasses must implement this to provide their specific recipe source.
     *
     * @return List of engrave results for ItemBlock
     */
    protected abstract List<EngraveItemResult> getItemBlockRecipes();
    
    protected abstract List<EngraveTextResult> getTextBlockRecipes();

    public void setupResultSlotServer(ItemStack resultItemStack) {
        ItemStack itemStack = resultItemStack.copy();
        if (itemStack.isItemEnabled(this.level.enabledFeatures())) {
            this.resultSlot.set(itemStack);
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    void setupResultSlot() {
        if (level.isClientSide) {
            if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipeIndex.get())) {
                IEngraveResult recipeHolder = this.recipes.get(this.selectedRecipeIndex.get());
                ItemStack itemStack = recipeHolder.getResultItem().copy();
                if (itemStack.isItemEnabled(this.level.enabledFeatures())) {
                    TransformDataListPacket.sendToServerSide(recipeHolder);
                    this.resultSlot.set(itemStack);
                } else {
                    this.resultSlot.set(ItemStack.EMPTY);
                }
            } else {
                this.resultSlot.set(ItemStack.EMPTY);
            }
            this.broadcastChanges();
        }
    }

    public void registerUpdateListener(Runnable listener) {
        this.slotUpdateListener = listener;
    }

    public BLOCK_TYPE getBlockType() {
        return this.useItemRecipes;
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            Item item = itemStack2.getItem();
            itemStack = itemStack2.copy();
            if (index == RESULT_SLOT) {
                item.onCraftedBy(itemStack2, player.level(), player);
                if (!this.moveItemStackTo(itemStack2, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == INPUT_SLOT) {
                // Move from input slot to player inventory
                if (!this.moveItemStackTo(itemStack2, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to input slot
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
            this.broadcastChanges();
        }
        return itemStack;
    }

    public void removed(@NotNull Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(1);
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.container));
    }

    public enum BLOCK_TYPE {
        BLOCK,ITEM,TEXT
    }
}
