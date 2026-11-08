package com.yuushya.modelling.gui.modeling;

import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 顶栏的工具按钮条：复制 / 粘贴 / 保存 / 统计。
 * <p>
 * 自绘而非用 {@code Button}，因为顶栏高度只有 24px、还需要与右侧的模式显示共存；
 * 悬停高亮与点击判定都在这里完成，界面只需转发鼠标事件。
 */
public final class ModelingToolbar {

    /** 顶栏上的动作。 */
    public enum Action {
        COPY("📤", "gui.yuushya.modeling.action.copy"),
        PASTE("📥", "gui.yuushya.modeling.action.paste"),
        SAVE("💾", "gui.yuushya.modeling.action.save"),
        STATS("📊", "gui.yuushya.modeling.action.stats");

        final String symbol;
        final String tooltipKey;

        Action(String symbol, String tooltipKey) {
            this.symbol = symbol;
            this.tooltipKey = tooltipKey;
        }

        public Component tooltip() {
            return Component.translatable(this.tooltipKey);
        }
    }

    public interface Listener {
        void onAction(Action action);
    }

    private static final int BUTTON = 18;
    private static final int GAP = 2;
    private static final int BG = 0xFF2A2A2A;
    private static final int BG_HOVER = 0xFF3C3C3C;
    private static final int BORDER = 0xFF0B0B0B;
    private static final int TEXT = 0xFFCCCCCC;
    private static final int TEXT_HOVER = 0xFFFFFFFF;
    private static final int STATS_TEXT = 0xFF8FD7B0;

    private final Listener listener;
    @Setter
    private ModelingLayout.Rect rect = new ModelingLayout.Rect(0, 0, 0, 0);
    private Action hovered;
    /** 📊 按钮右侧显示的面数；负数表示尚未统计。
     * -- SETTER --
     * 更新面数统计（负数 = 清除）。
     */
    @Setter
    private long faceCount = -1;

    public ModelingToolbar(Listener listener) {
        this.listener = listener;
    }

    /** 模型数据变化后统计结果失效。 */
    public void invalidateStats() {
        this.faceCount = -1;
    }

    private int buttonX(int index) {
        return this.rect.x() + 4 + index * (BUTTON + GAP);
    }

    private int buttonY() {
        return this.rect.y() + (this.rect.height() - BUTTON) / 2;
    }

    /** @return 命中的按钮，未命中返回 {@code null} */
    private Action at(double mouseX, double mouseY) {
        Action[] actions = Action.values();
        int y = buttonY();
        if (mouseY < y || mouseY >= y + BUTTON) {
            return null;
        }
        for (int i = 0; i < actions.length; i++) {
            int x = buttonX(i);
            if (mouseX >= x && mouseX < x + BUTTON) {
                return actions[i];
            }
        }
        return null;
    }

    public void updateHover(double mouseX, double mouseY) {
        this.hovered = at(mouseX, mouseY);
    }

    /** @return 是否消费了本次点击 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        Action action = at(mouseX, mouseY);
        if (action == null) {
            return false;
        }
        this.listener.onAction(action);
        return true;
    }

    public String hoveredTooltip() {
        return this.hovered == null ? null : this.hovered.tooltip().getString();
    }

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        if (this.rect.width() <= 0 || this.rect.height() <= 0) {
            return;
        }
        updateHover(mouseX, mouseY);
        Action[] actions = Action.values();
        int y = buttonY();
        for (int i = 0; i < actions.length; i++) {
            int x = buttonX(i);
            boolean hover = this.hovered == actions[i];
            guiGraphics.fill(x, y, x + BUTTON, y + BUTTON, hover ? BG_HOVER : BG);
            guiGraphics.fill(x, y, x + BUTTON, y + 1, BORDER);
            guiGraphics.fill(x, y + BUTTON - 1, x + BUTTON, y + BUTTON, BORDER);
            guiGraphics.fill(x, y, x + 1, y + BUTTON, BORDER);
            guiGraphics.fill(x + BUTTON - 1, y, x + BUTTON, y + BUTTON, BORDER);
            String symbol = actions[i].symbol;
            guiGraphics.drawString(font, symbol,
                    x + (BUTTON - font.width(symbol)) / 2, y + (BUTTON - 8) / 2,
                    hover ? TEXT_HOVER : TEXT, false);
        }

        renderFaceCount(guiGraphics, font, actions.length, y);
    }

    /** 面数紧跟最后一个按钮之后，左对齐到它的右侧间距处。 */
    private void renderFaceCount(GuiGraphics guiGraphics, Font font, int buttonCount, int y) {
        if (this.faceCount < 0) {
            return;
        }
        String text = String.valueOf(this.faceCount);
        int x = buttonX(buttonCount - 1) + BUTTON + GAP + 3;
        guiGraphics.drawString(font, text, x, y + (BUTTON - 8) / 2, STATS_TEXT, false);
    }
}
