package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TextIconList extends AbstractTransformIconList<TransformTextData, TextIconList.Entry> {

    protected final TextBlockScreen screen;

    public TextIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                        List<TransformTextData> transformDataList, TextBlockScreen textBlockScreen) {
        super(minecraft, width, height, x, y0, itemWidth, itemHeight, transformDataList);
        this.screen = textBlockScreen;
    }

    @Override
    protected Entry createEntry(int slot) {
        return new Entry(this, slot);
    }

    @Override
    protected TransformTextData defaultData() {
        return new TransformTextData();
    }

    @Override
    protected boolean isSlotEmpty(int slot) {
        return transformDataList.get(slot).textLines.isEmpty();
    }

    @Override
    protected void selectSlot(int slot) {
        this.screen.setSlot(slot);
    }

    public class Entry extends AbstractTransformIconList.Entry<TransformTextData, Entry> {

        public Entry(TextIconList parent, int slot) {
            super(parent, slot);
        }

        public List<String> updateRenderTextLines() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot).textLines : new ArrayList<>();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            List<String> textLines = updateRenderTextLines();
            Font font = Minecraft.getInstance().font;

            // Render text preview (first line or placeholder)
            // Deserialize JSON string to Component and extract plain text
            String previewText = "[Empty]";
            if (!textLines.isEmpty()) {
                try {
                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        Component component = Component.Serializer.fromJson(textLines.get(0), level.registryAccess());
                        if (component != null) {
                            previewText = component.getString();
                        }
                    }
                } catch (Exception e) {
                    // If deserialization fails, use raw string as fallback
                    previewText = textLines.get(0);
                }
            }
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
        public Component getNarration() {
            return Component.translatable("narrator.select", this.slot);
        }
    }
}
