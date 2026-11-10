package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 元素列表上的上下移按钮。
 * <p>
 * 与左侧工具条分开绘制：工具条管"增/删/复制/显隐"，这里是"调顺序"，
 * 放在面板标题行右侧，视觉上不干扰主要动作。
 */
public final class ModelingOrderButtons {

    public enum Direction {
        UP("▲", "gui.yuushya.modeling.order.up"),
        DOWN("▼", "gui.yuushya.modeling.order.down");

        final String symbol;
        final String tooltipKey;

        Direction(String symbol, String tooltipKey) {
            this.symbol = symbol;
            this.tooltipKey = tooltipKey;
        }
    }

    public interface Listener {
        void onMove(Direction direction);
    }

    private static final int SIZE = 12;
    private static final int GAP = 2;
    private static final int BG = 0xFF2A2A2A;
    private static final int BG_HOVER = 0xFF3C3C3C;
    private static final int BG_DISABLED = 0xFF1E1E1E;
    private static final int BORDER = 0xFF0B0B0B;
    private static final int TEXT = 0xFFCCCCCC;
    private static final int TEXT_HOVER = 0xFFFFFFFF;
    private static final int TEXT_DISABLED = 0xFF4A4A4A;

    private final Listener listener;
    private ModelingLayout.Rect rect = new ModelingLayout.Rect(0, 0, 0, 0);
    private Direction hovered;
    private boolean canUp;
    private boolean canDown;

    public ModelingOrderButtons(Listener listener) {
        this.listener = listener;
    }

    /** @param titleRow 面板标题行的矩形，按钮贴在它右端 */
    public void setRect(ModelingLayout.Rect titleRow) {
        this.rect = titleRow;
    }

    /** 由界面在每次渲染前告知当前选中位置能否上下移动。 */
    public void setBounds(boolean canUp, boolean canDown) {
        this.canUp = canUp;
        this.canDown = canDown;
    }

    private int buttonX(int index) {
        return this.rect.right() - 4 - (2 - index) * (SIZE + GAP);
    }

    private int buttonY() {
        return this.rect.y() + (this.rect.height() - SIZE) / 2;
    }

    private boolean enabled(Direction direction) {
        return direction == Direction.UP ? this.canUp : this.canDown;
    }

    private Direction at(double mouseX, double mouseY) {
        int y = buttonY();
        if (mouseY < y || mouseY >= y + SIZE) {
            return null;
        }
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            int x = buttonX(i);
            if (mouseX >= x && mouseX < x + SIZE) {
                return directions[i];
            }
        }
        return null;
    }

    public void updateHover(double mouseX, double mouseY) {
        Direction hit = at(mouseX, mouseY);
        this.hovered = hit != null && enabled(hit) ? hit : null;
    }

    /** @return 是否消费了本次点击 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        Direction hit = at(mouseX, mouseY);
        if (hit == null || !enabled(hit)) {
            return false;
        }
        this.listener.onMove(hit);
        return true;
    }

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        if (this.rect.width() <= 0 || this.rect.height() <= 0) {
            return;
        }
        updateHover(mouseX, mouseY);
        int y = buttonY();
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[i];
            int x = buttonX(i);
            boolean on = enabled(direction);
            boolean hover = on && this.hovered == direction;
            int bg = !on ? BG_DISABLED : (hover ? BG_HOVER : BG);
            guiGraphics.fill(x, y, x + SIZE, y + SIZE, bg);
            guiGraphics.fill(x, y, x + SIZE, y + 1, BORDER);
            guiGraphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, BORDER);
            guiGraphics.fill(x, y, x + 1, y + SIZE, BORDER);
            guiGraphics.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, BORDER);
            String symbol = direction.symbol;
            guiGraphics.drawString(font, symbol,
                    x + (SIZE - font.width(symbol)) / 2, y + (SIZE - 8) / 2,
                    !on ? TEXT_DISABLED : (hover ? TEXT_HOVER : TEXT), false);
        }
    }

    /** 悬停按钮的提示文本，未悬停返回 {@code null}。 */
    public String hoveredTooltip() {
        return this.hovered == null ? null
                : Component.translatable(this.hovered.tooltipKey).getString();
    }

    public int hoveredX() {
        if (this.hovered == null) {
            return 0;
        }
        return buttonX(this.hovered.ordinal()) + SIZE;
    }

    public int hoveredY() {
        return buttonY();
    }
}
