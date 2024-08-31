package com.yuushya.modelling.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TooltipUtils {
    public static Button.OnTooltip create(Screen screen,Component component){
        return new Button.OnTooltip() {
            public void onTooltip(Button button, PoseStack poseStack, int i, int j) {
                screen.renderTooltip(poseStack, component, i, j);
            }

            public void narrateTooltip(Consumer<Component> contents) {
                contents.accept(component);
            }
        };
    }

    @FunctionalInterface
    public interface TooltipSupplier<T> extends Function<T, List<FormattedCharSequence>> {
    }
}
