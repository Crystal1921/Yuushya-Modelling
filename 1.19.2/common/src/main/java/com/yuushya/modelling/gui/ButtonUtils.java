package com.yuushya.modelling.gui;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.Button.OnTooltip;

public class ButtonUtils {
    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        @Nullable
        private OnTooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public Builder tooltip(@Nullable OnTooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }



        public net.minecraft.client.gui.components.Button build() {
            net.minecraft.client.gui.components.Button button = new net.minecraft.client.gui.components.Button(this.x, this.y, this.width, this.height, this.message, this.onPress, this.tooltip);
            return button;
        }
    }
}
