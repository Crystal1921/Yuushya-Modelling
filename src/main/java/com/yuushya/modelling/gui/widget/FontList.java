package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FontList extends ObjectSelectionList<FontList.Entry> {

    protected final List<ResourceLocation> fontList;
    private final TextBlockScreen parentScreen;
    private int itemHeight;
    private int itemWidth;
    private boolean visible = true;

    public FontList(Minecraft minecraft, TextBlockScreen textBlockScreen, int width, int height, int x, int y0, int itemHeight) {
        super(minecraft, width, height, y0, y0 + height, itemHeight);
        this.setLeftPos(x);
        this.parentScreen = textBlockScreen;
        this.fontList = new ArrayList<>();
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = width;
        this.itemHeight = itemHeight;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
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
        return Minecraft.DEFAULT_FONT;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < this.children().size()) {
            this.setSelected(this.children().get(slot));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (visible) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return visible && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return visible && super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return visible && super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount) {
        return visible && super.mouseScrolled(mouseX, mouseY, horizontalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return visible && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return visible && super.charTyped(codePoint, modifiers);
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
        return this.getLeft() + 4;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getLeft() + this.getWidth() - 4;
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
            ResourceLocation fontLoc = getFont();
            FontSet fontSet = Minecraft.getInstance().fontManager.fontSets.get(fontLoc);
            Font font = new Font(resourceLocation -> fontSet, true);
            String fontName = fontSet.name.toString();
            int textY = y + (itemHeight - font.lineHeight) / 2;
            guiGraphics.drawString(font, fontName, x, textY, 0xFFFFFF, true);

            // Render selection indicator
            if (isMouseOver || this == FontList.this.getSelected()) {
                guiGraphics.fill(x - 1, y - 1, x + itemWidth + 1, y + itemHeight + 1, 0x80FFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.parent.setSelected(this);
            this.parent.parentScreen.textEditBox.setFontTextStyle();
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }
}
