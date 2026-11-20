package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 建模界面的面板绘制：顶栏、底栏、左侧元素列表、右上元素属性、右下变换属性。
 * <p>
 * 只负责画，不持有状态，也不处理交互。中间视口由 {@link ModelingViewport} 单独绘制。
 */
public final class ModelingChrome {

    public static final int CHROME_BG = 0xFF1B1B1B;
    public static final int PANEL_BG = 0xFF232323;
    public static final int PANEL_BORDER = 0xFF0B0B0B;
    public static final int PANEL_TITLE = 0xFF9A9A9A;
    public static final int TEXT = 0xFFC6C6C6;

    private ModelingChrome() {
    }

    /**
     * 绘制全部面板底色与标题。
     *
     * @param active 顶栏中间显示的标题（例如当前模型名），可为 {@code null}
     * @param status 底栏左侧显示的状态文本，可为 {@code null}
     */
    public static void render(GuiGraphics guiGraphics, Font font, ModelingLayout layout,
                              Component active, Component status) {
            render(guiGraphics, font, layout, active, status,
                    "gui.yuushya.modeling.outliner", "gui.yuushya.modeling.properties");
        }

        /**
         * 同上，但左右两块面板的标题可以自定义（碰撞箱雕刻界面用「工具 / 选项」）。
         */
        public static void render(GuiGraphics guiGraphics, Font font, ModelingLayout layout,
                                  Component active, Component status,
                                  String leftTitleKey, String rightTitleKey) {
        ModelingLayout.Rect header = layout.header;
        guiGraphics.fill(header.x(), header.y(), header.right(), header.bottom(), CHROME_BG);
        guiGraphics.fill(header.x(), header.bottom() - 1, header.right(), header.bottom(), PANEL_BORDER);

        ModelingLayout.Rect footer = layout.footer;
        guiGraphics.fill(footer.x(), footer.y(), footer.right(), footer.bottom(), CHROME_BG);
        guiGraphics.fill(footer.x(), footer.y(), footer.right(), footer.y() + 1, PANEL_BORDER);

        panel(guiGraphics, layout.elements);
        panel(guiGraphics, layout.right);

        title(guiGraphics, font, layout.elements, leftTitleKey);
                title(guiGraphics, font, layout.right, rightTitleKey);

        if (active != null) {
            guiGraphics.drawString(font, active, (header.right() - font.width(active)) / 2,
                    header.y() + (header.height() - 8) / 2, TEXT, false);
        }
        if (status != null) {
            guiGraphics.drawString(font, status, footer.x() + 5, footer.y() + 4, TEXT, false);
        }
    }

    private static void title(GuiGraphics guiGraphics, Font font, ModelingLayout.Rect rect, String key) {
        guiGraphics.drawString(font, Component.translatable(key), rect.x() + 5, rect.y() + 4, PANEL_TITLE, false);
    }

    private static void panel(GuiGraphics guiGraphics, ModelingLayout.Rect rect) {
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), PANEL_BG);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), PANEL_BORDER);
    }
}
