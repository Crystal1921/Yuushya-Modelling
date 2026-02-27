package com.yuushya.modelling.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

/**
 * 抽象颜色编辑Screen，提供颜色编辑功能的通用接口
 * 用于ItemBlockScreen、TextBlockScreen等需要颜色编辑功能的Screen
 */
public abstract class AbstractColorScreen extends Screen {
    protected int color = Color.WHITE.getRGB();

    protected AbstractColorScreen(Component title) {
        super(title);
    }

    /**
     * 获取字体实例，用于颜色编辑器绘制文本
     */
    public abstract Font getColorFont();

    /**
     * 获取颜色编辑框，用于显示和编辑十六进制颜色值
     */
    public abstract EditBox getColorEditBox();

    /**
     * 更新颜色数据到实体
     * @param colorValue 颜色值 (RGB整数)
     */
    public abstract void updateColorData(int colorValue);

    /**
     * 设置焦点到指定组件
     * @param widget 要设置焦点的组件
     */
    public abstract void setColorFocused(AbstractWidget widget);

    /**
     * 设置拖拽状态
     * @param dragging 是否正在拖拽
     */
    public abstract void setColorDragging(boolean dragging);
}
