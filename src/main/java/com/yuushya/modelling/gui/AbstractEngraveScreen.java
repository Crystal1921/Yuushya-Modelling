//all right reserved

package com.yuushya.modelling.gui;

import com.yuushya.modelling.gui.engrave.IEngraveResult;
import com.yuushya.modelling.utils.DeprecatedMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Abstract base class for Engrave-style screens (EngraveScreen and HistoryScreen).
 * This class contains all the common rendering and interaction logic for recipe selection screens.
 *
 * @param <T> The menu type that extends AbstractEngraveMenu
 */
public abstract class AbstractEngraveScreen<T extends AbstractEngraveMenu> extends AbstractContainerScreen<T> {
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
    private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
    private static final int SCROLLER_FULL_HEIGHT = 54;
    private static final int RECIPES_X = 52;
    private static final int RECIPES_Y = 14;

    private float scrollOffs;
    /**
     * Is {@code true} if the player clicked on the scroll wheel in the GUI.
     */
    private boolean scrolling;
    /**
     * The index of the first recipe to display.
     * The number of recipes displayed at any time is 12 (4 recipes per row, and 3 rows).
     * If the player scrolled down one row, this value would be 4 (representing the index of the first slot on the second row).
     */
    private int startIndex;
    private boolean displayRecipes;

    public AbstractEngraveScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        menu.registerUpdateListener(this::containerChanged);
        --this.titleLabelY;
    }

    //TODO 这里注释掉不知道有没有影响
//    @Override
//    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
//        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
//        this.renderTooltip(guiGraphics, mouseX, mouseY);
//    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int i = this.leftPos;
        int j = this.topPos;
        DeprecatedMethod.blit(guiGraphics, BG_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int k = (int) (41.0f * this.scrollOffs);
        Identifier texture = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        DeprecatedMethod.blitSprite(guiGraphics, texture, i + 119, j + SCROLLER_HEIGHT + k, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        int l = this.leftPos + RECIPES_X;
        int m = this.topPos + RECIPES_Y;
        int n = this.startIndex + SCROLLER_WIDTH;
        this.renderButtons(guiGraphics, mouseX, mouseY, l, m, n);
        this.renderRecipes(guiGraphics, l, m, n);
    }

//    @Override
//    protected void renderTooltip(GuiGraphicsExtractor guiGraphics, int x, int y) {
//        super.renderTooltip(guiGraphics, x, y);
//        if (this.displayRecipes) {
//            int i = this.leftPos + RECIPES_X;
//            int j = this.topPos + RECIPES_Y;
//            int k = this.startIndex + SCROLLER_WIDTH;
//            List<IEngraveResult> list = this.menu.getRecipes();
//            for (int l = this.startIndex; l < k && l < this.menu.getNumRecipes(); ++l) {
//                int m = l - this.startIndex;
//                int n = i + m % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
//                int o = j + m / 4 * RECIPES_IMAGE_SIZE_HEIGHT + 2;
//                if (x < n || x >= n + RECIPES_IMAGE_SIZE_WIDTH || y < o || y >= o + RECIPES_IMAGE_SIZE_HEIGHT) continue;
//                guiGraphics.renderTooltip(this.font, list.get(l).getResultItem(), x, y);
//            }
//        }
//    }

    private void renderButtons(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int x, int y, int lastVisibleElementIndex) {
        for (int i = this.startIndex; i < lastVisibleElementIndex && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            int l = j / RECIPES_COLUMNS;
            int m = y + l * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            Identifier Identifier = i == this.menu.getSelectedRecipeIndex() ? RECIPE_SELECTED_SPRITE : (mouseX >= k && mouseY >= m && mouseX < k + RECIPES_IMAGE_SIZE_WIDTH && mouseY < m + RECIPES_IMAGE_SIZE_HEIGHT ? RECIPE_HIGHLIGHTED_SPRITE : RECIPE_SPRITE);
            DeprecatedMethod.blitSprite(guiGraphics, Identifier, k, m - 1, RECIPES_IMAGE_SIZE_WIDTH, RECIPES_IMAGE_SIZE_HEIGHT);
        }
    }

    private void renderRecipes(GuiGraphicsExtractor guiGraphics, int x, int y, int startIndex) {
        List<IEngraveResult> list = this.menu.getRecipes();
        for (int i = this.startIndex; i < startIndex && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            int l = j / RECIPES_COLUMNS;
            int m = y + l * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            guiGraphics.item(list.get(i).getResultItem(), k, m);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.scrolling = false;
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.displayRecipes) {
            int i = this.leftPos + RECIPES_X;
            int j = this.topPos + RECIPES_Y;
            int k = this.startIndex + SCROLLER_WIDTH;
            for (int l = this.startIndex; l < k; ++l) {
                int m = l - this.startIndex;
                double d = mouseX - (double) (i + m % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH);
                double e = mouseY - (double) (j + m / RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_HEIGHT);
                if (!(d >= 0.0) || !(e >= 0.0) || !(d < 16.0) || !(e < 18.0) || !this.menu.clickMenuButton(this.minecraft.player, l))
                    continue;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
                return true;
            }
            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (mouseX >= (double) i && mouseX < (double) (i + SCROLLER_WIDTH) && mouseY >= (double) j && mouseY < (double) (j + SCROLLER_FULL_HEIGHT)) {
                this.scrolling = true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + RECIPES_Y;
            int j = i + SCROLLER_FULL_HEIGHT;
            this.scrollOffs = ((float) event.y() - (float) i - 7.5f) / ((float) (j - i) - 15.0f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);
            this.startIndex = (int) ((double) (this.scrollOffs * (float) this.getOffscreenRows()) + 0.5) * RECIPES_COLUMNS;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int i = this.getOffscreenRows();
            float f = (float) scrollY / (float) i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0f, 1.0f);
            this.startIndex = (int) ((double) (this.scrollOffs * (float) i) + 0.5) * RECIPES_COLUMNS;
        }
        return true;
    }

    private boolean isScrollBarActive() {
        return this.displayRecipes && this.menu.getNumRecipes() > SCROLLER_WIDTH;
    }

    protected int getOffscreenRows() {
        return (this.menu.getNumRecipes() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS;
    }

    /**
     * Called every time this screen's container is changed (is marked as dirty).
     */
    private void containerChanged() {
        this.displayRecipes = this.menu.hasInputItem();
        if (!this.displayRecipes) {
            this.scrollOffs = 0.0f;
            this.startIndex = 0;
        }
    }
}
