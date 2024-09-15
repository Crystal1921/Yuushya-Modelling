package com.yuushya.modelling.gui.engrave;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

@Environment(value=EnvType.CLIENT)
public class EngraveScreen
        extends AbstractContainerScreen<EngraveMenu> {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
    private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
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
     * The number of recipes displayed at any time is 12 (4 recipes per row, and 3 rows). If the player scrolled down one row, this value would be 4 (representing the index of the first slot on the second row).
     */
    private int startIndex;
    private boolean displayRecipes;

    public EngraveScreen(EngraveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        menu.registerUpdateListener(this::containerChanged);
        --this.titleLabelY;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(BG_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int k = (int)(41.0f * this.scrollOffs);
        ResourceLocation resourceLocation = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        guiGraphics.blitSprite(resourceLocation, i + 119, j + SCROLLER_HEIGHT + k, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        int l = this.leftPos + RECIPES_X;
        int m = this.topPos + RECIPES_Y;
        int n = this.startIndex + SCROLLER_WIDTH;
        this.renderButtons(guiGraphics, mouseX, mouseY, l, m, n);
        this.renderRecipes(guiGraphics, l, m, n);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (this.displayRecipes) {
            int i = this.leftPos + RECIPES_X;
            int j = this.topPos + RECIPES_Y;
            int k = this.startIndex + SCROLLER_WIDTH;
            List<EngraveItemResult> list = this.menu.getRecipes();
            for (int l = this.startIndex; l < k && l < this.menu.getNumRecipes(); ++l) {
                int m = l - this.startIndex;
                int n = i + m % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
                int o = j + m / 4 * RECIPES_IMAGE_SIZE_HEIGHT + 2;
                if (x < n || x >= n + RECIPES_IMAGE_SIZE_WIDTH || y < o || y >= o + RECIPES_IMAGE_SIZE_HEIGHT) continue;
                guiGraphics.renderTooltip(this.font, list.get(l).getResultItem(), x, y);
            }
        }
    }

    private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int lastVisibleElementIndex) {
        for (int i = this.startIndex; i < lastVisibleElementIndex && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            int l = j / RECIPES_COLUMNS;
            int m = y + l * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            ResourceLocation resourceLocation = i == this.menu.getSelectedRecipeIndex() ? RECIPE_SELECTED_SPRITE : (mouseX >= k && mouseY >= m && mouseX < k + RECIPES_IMAGE_SIZE_WIDTH && mouseY < m + RECIPES_IMAGE_SIZE_HEIGHT ? RECIPE_HIGHLIGHTED_SPRITE : RECIPE_SPRITE);
            guiGraphics.blitSprite(resourceLocation, k, m - 1, RECIPES_IMAGE_SIZE_WIDTH, RECIPES_IMAGE_SIZE_HEIGHT);
        }
    }

    private void renderRecipes(GuiGraphics guiGraphics, int x, int y, int startIndex) {
        List<EngraveItemResult> list = this.menu.getRecipes();
        for (int i = this.startIndex; i < startIndex && i < this.menu.getNumRecipes(); ++i) {
            int j = i - this.startIndex;
            int k = x + j % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            int l = j / RECIPES_COLUMNS;
            int m = y + l * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            guiGraphics.renderItem(list.get(i).getResultItem(), k, m);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        if (this.displayRecipes) {
            int i = this.leftPos + RECIPES_X;
            int j = this.topPos + RECIPES_Y;
            int k = this.startIndex + SCROLLER_WIDTH;
            for (int l = this.startIndex; l < k; ++l) {
                int m = l - this.startIndex;
                double d = mouseX - (double)(i + m % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH);
                double e = mouseY - (double)(j + m / RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_HEIGHT);
                if (!(d >= 0.0) || !(e >= 0.0) || !(d < 16.0) || !(e < 18.0) || !this.menu.clickMenuButton(this.minecraft.player, l)) continue;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
                return true;
            }
            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (mouseX >= (double)i && mouseX < (double)(i + SCROLLER_WIDTH) && mouseY >= (double)j && mouseY < (double)(j + SCROLLER_FULL_HEIGHT)) {
                this.scrolling = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + RECIPES_Y;
            int j = i + SCROLLER_FULL_HEIGHT;
            this.scrollOffs = ((float)mouseY - (float)i - 7.5f) / ((float)(j - i) - 15.0f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);
            this.startIndex = (int)((double)(this.scrollOffs * (float)this.getOffscreenRows()) + 0.5) * RECIPES_COLUMNS;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isScrollBarActive()) {
            int i = this.getOffscreenRows();
            float f = (float)scrollY / (float)i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0f, 1.0f);
            this.startIndex = (int)((double)(this.scrollOffs * (float)i) + 0.5) * RECIPES_COLUMNS;
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

