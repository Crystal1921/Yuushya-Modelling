package com.yuushya.modelling.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;

/**
 * 颜色编辑器（{@link ColorWidget}）对宿主界面的要求。
 * <p>
 * 只暴露色板真正会用到的能力，宿主界面可以自由选择继承体系（建模界面继承了
 * {@code AbstractModelingScreen}，旧的变换界面直接继承 {@code Screen}）。
 */
public interface ColorEditorHost {

    /** 绘制色板文字用的字体。 */
    Font getColorFont();

    /** 十六进制输入框；没有时返回 {@code null}。 */
    default EditBox getColorEditBox() {
        return null;
    }

    /** 色板选色的回写入口。 */
    default void updateColorData(int colorValue) {
    }

    /** 让宿主把焦点交给色板。 */
    void setColorFocused(AbstractWidget widget);

    /** 让宿主记录「正在拖动色板」，避免拖动过程中被别的逻辑抢走焦点。 */
    void setColorDragging(boolean dragging);
}