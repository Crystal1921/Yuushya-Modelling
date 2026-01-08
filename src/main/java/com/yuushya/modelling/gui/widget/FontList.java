package com.yuushya.modelling.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FontList extends ObjectSelectionList<FontList.Entry> {

    protected final List<ResourceLocation> fontList;
    private int itemHeight;
    private int itemWidth;

    public FontList(Minecraft minecraft, int width, int height, int x, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.fontList = new ArrayList<>();
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = width;
        this.itemHeight = itemHeight;
    }

    public void updateRenderList(List<ResourceLocation> fonts) {
        this.fontList.clear();
        this.fontList.addAll(fonts);
        this.clearEntries();
        for (int i = 0; i < fontList.size(); i++) {
            Entry entry = new Entry(this, i);
            this.addEntry(entry);
        }
    }

    public ResourceLocation getSelectedFont() {
        Entry selected = this.getSelected();
        if (selected != null && selected.slot < fontList.size()) {
            return fontList.get(selected.slot);
        }
        return null;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < this.children().size()) {
            this.setSelected(this.children().get(slot));
        }
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        super.setSelected(selected);
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    public int getRowLeft() {
        return this.getX() + 4;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.getWidth() - 4;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {

        private final FontList parent;
        private final int slot;
        private final Minecraft minecraft;

        public Entry(FontList parent, int slot) {
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        public ResourceLocation getFont() {
            if (parent.fontList.size() > slot) {
                return parent.fontList.get(slot);
            }
            return null;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ResourceLocation font = getFont();

            // Render font name
            Font fontRenderer = Minecraft.getInstance().font;
            if (font != null) {
                String fontName = font.toString();
                int textY = y + (itemHeight - fontRenderer.lineHeight) / 2;
                guiGraphics.drawString(fontRenderer, fontName, x, textY, 0xFFFFFF, true);
            }

            // Render selection indicator
            if (isMouseOver || this == FontList.this.getSelected()) {
                guiGraphics.fill(x - 1, y - 1, x + itemWidth + 1, y + itemHeight + 1, 0x80FFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.parent.setSelected(this);
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }
}
