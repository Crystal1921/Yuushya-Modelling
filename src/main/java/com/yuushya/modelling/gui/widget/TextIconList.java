package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.utils.DeprecatedMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TextIconList extends ObjectSelectionList<TextIconList.Entry> {

    protected final List<TransformTextData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final TextBlockScreen screen;
    private final int itemHeight;
    private final int itemWidth;

    public TextIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                        List<TransformTextData> transformDataList, TextBlockScreen textBlockScreen) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.screen = textBlockScreen;
        this.centerListVertically = false;
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
    protected int scrollBarX() {
        return this.getX() + this.getWidth() - 4;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {

        private final TextIconList parent;
        private final int slot;
        private boolean chosen = false;

        public Entry(TextIconList parent, int slot) {
            this.parent = parent;
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
        public void extractContent(GuiGraphicsExtractor guiGraphics, int x, int y, boolean hovered, float v) {
            List<String> textLines = updateRenderTextLines();
            Font font = Minecraft.getInstance().font;

            // Render text preview (first line or placeholder)
            // Deserialize JSON string to Component and extract plain text
            String previewText = "[Empty]";
            if (!textLines.isEmpty()) {
                try {
                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        Component component = DeprecatedMethod.fromJson(textLines.getFirst(), level.registryAccess());
                        if (component != null) {
                            previewText = component.getString();
                        }
                    }
                } catch (Exception e) {
                    // If deserialization fails, use raw string as fallback
                    previewText = textLines.getFirst();
                }
            }
            if (previewText.length() > 6) {
                previewText = previewText.substring(0, 6) + "...";
            }
            guiGraphics.text(font, previewText, getX() + 2, getY() + parent.itemHeight / 2 - font.lineHeight / 2, 0xFFFFFFFF);

            // Render index number
            String indexStr = String.valueOf(this.slot);
            int textX = getX() + parent.itemWidth - font.width(indexStr) - 2;
            int textY = getY() + 2;
            guiGraphics.text(font, indexStr, textX, textY, 0xFFFFFFFF);

            // Render line count
            String lineCountStr = "(" + textLines.size() + ")";
            guiGraphics.text(font, lineCountStr, getX() + 2, getY() + parent.itemHeight - font.lineHeight - 2, 0xFFAAAAAA);

            // Render selection indicator
            if (hovered || this == TextIconList.this.getSelected()) {
                guiGraphics.fill(getX() - 1, getY() - 1, getX() + parent.itemWidth + 1, getY() + parent.itemHeight + 1, 0x80FFFFFF);
            }

            // Render chosen indicator
            if (chosen) {
                guiGraphics.fill(getX(), getY(), getX() + parent.itemWidth, getY() + parent.itemHeight, 0x5FD85C2F);
            }

            // Render shown state indicator
            if (updateRenderShown()) {
                guiGraphics.fill(getX(), getY(), getX() + 3, getY() + parent.itemHeight, 0x8000FF00);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
