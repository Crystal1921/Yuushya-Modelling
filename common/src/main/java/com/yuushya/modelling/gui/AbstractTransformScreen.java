package com.yuushya.modelling.gui;

import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.gui.showblock.EditScreen;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.utils.ShareUtils;
import dev.architectury.platform.Platform;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Abstract base class for transform screens that provides common UI elements and functionality
 * for both ShowBlockScreen and ItemBlockScreen.
 */
public abstract class AbstractTransformScreen extends Screen {
    public static final int PER_HEIGHT = 20;
    public static final int SMALL_BUTTON_WIDTH = 10;
    protected static final int TOP = 10;
    protected static final int RIGHT_COLUMN_X = 2;
    protected static final int RIGHT_BAR_WIDTH = PER_HEIGHT;
    protected static final int RIGHT_LIST_WIDTH = 40;
    protected static final int RIGHT_LIST_PER_HEIGHT = 45;
    protected static final int RIGHT_LIST_TOP = TOP + PER_HEIGHT + 5;
    protected static final int RIGHT_LIST_HEIGHT = 3 * RIGHT_LIST_PER_HEIGHT + 2;
    protected static final int RIGHT_LIST_BOTTOM = RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT;
    protected static final int RIGHT_STATE_PANEL_Y = RIGHT_LIST_BOTTOM + 5;
    protected static final int RIGHT_STATE_INFORM_X = RIGHT_COLUMN_X + RIGHT_LIST_WIDTH + 3;

    protected int slot;
    protected CycleButton<Mode> modeButton;
    protected CycleButton<Boolean> shownStateButton;

    protected AbstractTransformScreen() {
        super(GameNarrator.NO_TITLE);
    }

    // i \in [1,...]
    protected static int top(int i, int offset) {
        return TOP + PER_HEIGHT + 10 + PER_HEIGHT * i + offset;
    }

    protected int leftColumnX() {
        return this.width / 4 * 3 + 10;
    }

    protected int leftColumnWidth() {
        return this.width / 4 - 20;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No background rendering
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void checkModLack(ShareUtils.ShareInformation shareInformation) {
        List<String> unLoaded = shareInformation.mods().stream().filter(id -> !Platform.getModIds().contains(id)).toList();
        Minecraft.getInstance().getToasts().addToast(
                SystemToast.multiline(Minecraft.getInstance(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.literal("Mod Lack"), Component.literal(String.join(", ", unLoaded)))
        );
    }

    protected String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }

    protected void setClipboard(String clipboardValue) {
        if (this.minecraft != null) {
            TextFieldHelper.setClipboardContents(this.minecraft, clipboardValue);
        }
    }

    // Abstract methods that must be implemented by subclasses
    public abstract void setSlot(int slot);
    protected abstract BlockPos getBlockPos();
    protected abstract String getTranslationPrefix();
    protected abstract void updateTransformData(Object transformType, Double number);
    protected abstract void initializeTransformComponents();
    protected abstract void initializeListWidget();
    protected abstract Button createAddButton();
    protected abstract Button createRemoveButton();
    protected abstract Button createReplaceButton();
    protected abstract Button createCopyButton();
    protected abstract Button createPasteButton();
    protected abstract Button createSaveButton();

    public enum Mode implements StringRepresentable {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit");

        private final String name;
        @Getter
        private final Component symbol;

        Mode(String name) {
            this.name = name;
            this.symbol = Component.translatable("gui.showBlockScreen.mode." + name);
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}