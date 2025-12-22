package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TextIconList extends ObjectSelectionList<TextIconList.Entry> {

    protected final List<TransformTextData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final TextBlockScreen screen;
    private int itemHeight;
    private int itemWidth;

    public TextIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                        List<TransformTextData> transformDataList, TextBlockScreen textBlockScreen) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.screen = textBlockScreen;
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.updateRenderList();
    }

    public void updateRenderList() {
        this.clearEntries();
        for (int i = 0; i < transformDataList.size(); i++) {
            Entry entry = new Entry(this, i);
            this.addEntry(entry);
        }
    }

    public void addSlot() {
        if (this.getSelected() != null && transformDataList.get(this.getSelected().slot).textLines.isEmpty()) {
            return;
        }
        if (children().size() == transformDataList.size()) {
            TextIconList.Entry entry = new TextIconList.Entry(this, transformDataList.size());
            this.addEntry(entry);
            this.setSelected(entry);
        }
    }

    // getChosenOne returns the index of the last selected Entry
    public int getChosenOne() {
        if (!this.chosen.isEmpty()) {
            return this.chosen.getLast().slot;
        }
        return -1;
    }

    @Override
    public void setSelected(@Nullable TextIconList.Entry selected) {
        super.setSelected(selected);
        if (selected != null) {
            this.screen.setSlot(selected.slot);
        }
    }

    public void setSelectedSlot(int slot) {
        this.setSelected(this.children().get(slot));
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.getWidth() - 4;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {

        private final TextIconList parent;
        private final int slot;
        private final Minecraft minecraft;
        private boolean chosen = false;

        public Entry(TextIconList parent, int slot) {
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        public TransformTextData getTransformData() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot) : new TransformTextData();
        }

        public List<String> updateRenderTextLines() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot).textLines : new ArrayList<>();
        }

        public boolean updateRenderShown() {
            return parent.transformDataList.size() <= slot || parent.transformDataList.get(slot).isShown;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            List<String> textLines = updateRenderTextLines();
            Font font = Minecraft.getInstance().font;

            // Render text preview (first line or placeholder)
            String previewText = textLines.isEmpty() ? "[Empty]" : textLines.get(0);
            if (previewText.length() > 6) {
                previewText = previewText.substring(0, 6) + "...";
            }
            guiGraphics.drawString(font, previewText, x + 2, y + itemHeight / 2 - font.lineHeight / 2, 0xFFFFFF, false);

            // Render index number
            String indexStr = String.valueOf(this.slot);
            int textX = x + itemWidth - font.width(indexStr) - 2;
            int textY = y + 2;
            guiGraphics.drawString(font, indexStr, textX, textY, 0xFFFFFF, true);

            // Render line count
            String lineCountStr = "(" + textLines.size() + ")";
            guiGraphics.drawString(font, lineCountStr, x + 2, y + itemHeight - font.lineHeight - 2, 0xAAAAAA, false);

            // Render selection indicator
            if (isMouseOver || this == TextIconList.this.getSelected()) {
                guiGraphics.fill(x - 1, y - 1, x + itemWidth + 1, y + itemHeight + 1, 0x80FFFFFF);
            }

            // Render chosen indicator
            if (chosen) {
                guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, 0x5FD85C2F);
            }

            // Render shown state indicator
            if (updateRenderShown()) {
                guiGraphics.fill(x, y, x + 3, y + itemHeight, 0x8000FF00);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            TextIconList.Entry previousSelected = this.parent.getSelected();
            this.parent.setSelected(this);

            if (previousSelected == this) {
                if (this.chosen) {
                    this.chosen = false;
                    this.parent.chosen.remove(this);
                } else {
                    this.chosen = true;
                    this.parent.chosen.add(this);
                }
            }

            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.slot);
        }
    }
}
