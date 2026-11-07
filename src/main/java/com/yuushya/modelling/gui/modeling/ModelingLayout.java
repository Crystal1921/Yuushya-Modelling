package com.yuushya.modelling.gui.modeling;

import net.minecraft.util.Mth;

/**
 * 建模界面的区域划分。
 * <p>
 * 固定为：顶栏 / 底栏 / 左侧元素列表 / 右上元素属性 / 右下变换属性 / 中间 3D 视口。
 * 视口是实心背景的独立视图，不是透出的世界。所有尺寸都在这里算好，界面代码只读不算。
 */
public final class ModelingLayout {

    public static final int HEADER_HEIGHT = 24;
    public static final int FOOTER_HEIGHT = 16;
    /** 面板标题行高度。 */
    public static final int PANEL_TITLE_HEIGHT = 14;
    /** 视口最小宽度，小于它时优先压缩两侧面板。 */
    public static final int MIN_VIEWPORT_WIDTH = 120;
    public static final int MIN_LEFT_WIDTH = 72;
    public static final int MIN_RIGHT_WIDTH = 104;
    /** 元素列表行高。 */
    public static final int OUTLINER_ROW_HEIGHT = 26;
    /** 右上"元素属性"占内容区高度的比例。 */
    private static final float ELEMENT_PROPERTIES_RATIO = 0.45F;

    /** 一块矩形区域。 */
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    public final Rect header;
    public final Rect footer;
    /** 左侧元素列表。 */
    public final Rect elements;
    /** 右上元素属性（方块状态等）。 */
    public final Rect elementProperties;
    /** 右下变换属性（位移/旋转/缩放/亮度）。 */
    public final Rect transform;
    /** 中间 3D 视口。 */
    public final Rect viewport;

    public ModelingLayout(int width, int height) {
        int contentHeight = Math.max(40, height - HEADER_HEIGHT - FOOTER_HEIGHT);

        int left = Mth.clamp(width / 4, MIN_LEFT_WIDTH, 140);
        int right = Mth.clamp(width / 3, MIN_RIGHT_WIDTH, 180);

        // 视口优先：空间不足时先压左面板，再压右面板
        int overflow = left + right + MIN_VIEWPORT_WIDTH - width;
        if (overflow > 0) {
            int shrink = Math.min(overflow, left - MIN_LEFT_WIDTH);
            left -= shrink;
            overflow -= shrink;
        }
        if (overflow > 0) {
            right -= Math.min(overflow, right - MIN_RIGHT_WIDTH);
        }

        int rightX = width - right;
        int minElementProperties = PANEL_TITLE_HEIGHT + 2 * 20;
        int elementPropertiesHeight = Mth.clamp(Math.round(contentHeight * ELEMENT_PROPERTIES_RATIO),
                minElementProperties, Math.max(minElementProperties, contentHeight / 2));

        this.header = new Rect(0, 0, width, HEADER_HEIGHT);
        this.footer = new Rect(0, height - FOOTER_HEIGHT, width, FOOTER_HEIGHT);
        this.elements = new Rect(0, HEADER_HEIGHT, left, contentHeight);
        this.elementProperties = new Rect(rightX, HEADER_HEIGHT, right, elementPropertiesHeight);
        this.transform = new Rect(rightX, HEADER_HEIGHT + elementPropertiesHeight, right,
                Math.max(0, contentHeight - elementPropertiesHeight));
        this.viewport = new Rect(left, HEADER_HEIGHT, Math.max(0, rightX - left), contentHeight);
    }
}
