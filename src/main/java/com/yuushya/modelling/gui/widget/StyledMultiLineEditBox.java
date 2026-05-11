package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class StyledMultiLineEditBox extends AbstractTextAreaWidget {
    private static final int CURSOR_COLOR = -3092272;
    private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
    private static final int TEXT_COLOR = -2039584;

    private final Font font;
    private final Component placeholder;
    private final StyledMultilineTextField textField;
    private final int textColor;
    private final boolean textShadow;
    private final int cursorColor;
    private @Nullable IMEPreeditOverlay preeditOverlay;
    private final CycleButton<Boolean> boldButton;
    private final CycleButton<Boolean> italicButton;
    private final CycleButton<Boolean> underlineButton;
    private final CycleButton<Boolean> strikethroughButton;
    private final CycleButton<Boolean> obfuscatedButton;
    private final ColorWidget colorWidget;
    private final FontList fontList;
    private final TextBlockScreen textBlockScreen;
    private long focusedTime = Util.getMillis();

    public StyledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message, TextBlockScreen textBlockScreen) {
        this(font, x, y, width, height, placeholder, message, TEXT_COLOR, true, CURSOR_COLOR, true, true, textBlockScreen);
    }

    private StyledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component narration, int textColor, boolean textShadow, int cursorColor, boolean showBackground, boolean showDecorations, TextBlockScreen textBlockScreen) {
        super(x, y, width, height, narration, AbstractScrollArea.defaultSettings(4), showBackground, showDecorations);
        this.font = font;
        this.placeholder = placeholder;
        this.textShadow = textShadow;
        this.textColor = textColor;
        this.cursorColor = cursorColor;
        this.textField = new StyledMultilineTextField(font, width - this.totalInnerPadding(), textBlockScreen);
        this.textField.setCursorListener(this::scrollToCursor);
        this.boldButton = textBlockScreen.boldButton;
        this.italicButton = textBlockScreen.italicButton;
        this.underlineButton = textBlockScreen.underlineButton;
        this.strikethroughButton = textBlockScreen.strikethroughButton;
        this.obfuscatedButton = textBlockScreen.obfuscatedButton;
        this.colorWidget = textBlockScreen.colorWidget;
        this.fontList = textBlockScreen.fontList;
        this.textBlockScreen = textBlockScreen;
    }

    // ==================== 配置方法 ====================

    public void setCharacterLimit(int characterLimit) {
        this.textField.setCharacterLimit(characterLimit);
    }

    public void setValueListener(Consumer<List<Component>> valueListener) {
        this.textField.setValueListener(valueListener);
    }

    /**
     * 获取纯文本值
     */
    public String getValue() {
        return this.textField.getPlainText();
    }

    /**
     * 设置纯文本值（自动转换为无样式 Component）
     */
    public void setValue(String fullText) {
        this.textField.setValue(List.of(Component.literal(fullText)));
    }

    public void setValue(String value, boolean allowOverflowLineLimit) {
        this.textField.setValue(List.of(Component.literal(value)));
    }

    public void setValue(List<Component> components) {
        this.textField.setValue(components);
    }

    /**
     * 获取带样式的 Component 列表
     */
    public List<Component> getStyledValue() {
        return this.textField.getComponents();
    }

    /**
     * 设置带样式的 Component 列表
     */
    public void setStyledValue(List<Component> components) {
        this.textField.setValue(components);
    }

    // ==================== 渲染与交互 ====================

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (doubleClick) {
            // 双击选择单词（暂未实现）
        } else {
            this.textField.setSelecting(event.hasShiftDown());
            this.seekCursorScreen(event.x(), event.y());
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        this.textField.setSelecting(true);
        this.seekCursorScreen(event.x(), event.y());
        this.textField.setSelecting(event.hasShiftDown());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.textField.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.visible && this.isFocused() && event.isAllowedChatCharacter()) {
            Component component = Component.literal(event.codepointAsString())
                    .withStyle(Style.EMPTY
                            .withBold(boldButton.getValue())
                            .withItalic(italicButton.getValue())
                            .withUnderlined(underlineButton.getValue())
                            .withStrikethrough(strikethroughButton.getValue())
                            .withObfuscated(obfuscatedButton.getValue()));
            this.textField.insertStyledText(component);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        this.preeditOverlay = event != null ? new IMEPreeditOverlay(event, this.font, 10) : null;
        return true;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String value = this.textField.getPlainText();
        List<Component> components = this.textField.getComponents();

        if (value.isEmpty() && !this.isFocused()) {
            graphics.textWithWordWrap(this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), PLACEHOLDER_TEXT_COLOR);
        } else {
            int cursor = this.textField.cursor();
            boolean showCursor = this.isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - this.focusedTime);
            boolean needsValidCursorPos = this.preeditOverlay != null;
            boolean insertCursor = cursor < value.length();
            int cursorX = 0;
            int cursorY = 0;
            int drawTop = this.getInnerTop();
            int innerLeft = this.getInnerLeft();
            boolean hasDrawnCursor = false;

            // 渲染带样式的文本
            for (StyledMultilineTextField.StringView lineView : this.textField.iterateLines()) {
                boolean lineWithinVisibleBounds = this.withinContentAreaTopBottom(drawTop, drawTop + 9);

                if (!hasDrawnCursor && (needsValidCursorPos || showCursor) && insertCursor && cursor >= lineView.beginIndex() && cursor <= lineView.endIndex()) {
                    if (lineWithinVisibleBounds) {
                        String textBeforeCursor = value.substring(lineView.beginIndex(), cursor);
                        int textBeforeCursorPosRight = innerLeft + this.font.width(textBeforeCursor);
                        String textAfterCursor = value.substring(cursor, lineView.endIndex());

                        // 渲染带样式的文本（简化版，完整实现需要按 Component 分割渲染）
                        graphics.text(this.font, textBeforeCursor, innerLeft, drawTop, this.textColor, this.textShadow);
                        graphics.text(this.font, textAfterCursor, textBeforeCursorPosRight, drawTop, this.textColor, this.textShadow);

                        cursorX = textBeforeCursorPosRight;
                        cursorY = drawTop;

                        if (showCursor) {
                            TextCursorUtils.extractInsertCursor(graphics, textBeforeCursorPosRight, drawTop, this.cursorColor, 10);
                        }

                        hasDrawnCursor = true;
                    }
                } else if (lineWithinVisibleBounds) {
                    String substring = value.substring(lineView.beginIndex(), lineView.endIndex());
                    graphics.text(this.font, substring, innerLeft, drawTop, this.textColor, this.textShadow);
                    if ((needsValidCursorPos || showCursor) && !insertCursor) {
                        cursorX = innerLeft + this.font.width(substring);
                        cursorY = drawTop;
                    }
                }

                drawTop += 9;
            }

            if (showCursor && !insertCursor && this.withinContentAreaTopBottom(cursorY, cursorY + 9)) {
                TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, cursorY, this.cursorColor, this.textShadow);
            }

            // 渲染选择高亮
            if (this.textField.hasSelection()) {
                StyledMultilineTextField.StringView selection = this.textField.getSelected();
                int drawX = this.getInnerLeft();
                drawTop = this.getInnerTop();

                for (StyledMultilineTextField.StringView lineView : this.textField.iterateLines()) {
                    if (selection.beginIndex() > lineView.endIndex()) {
                        drawTop += 9;
                    } else {
                        if (lineView.beginIndex() > selection.endIndex()) {
                            break;
                        }

                        if (this.withinContentAreaTopBottom(drawTop, drawTop + 9)) {
                            int drawBegin = this.font.width(value.substring(lineView.beginIndex(), Math.max(selection.beginIndex(), lineView.beginIndex())));
                            int drawEnd;
                            if (selection.endIndex() > lineView.endIndex()) {
                                drawEnd = this.width - this.innerPadding();
                            } else {
                                drawEnd = this.font.width(value.substring(lineView.beginIndex(), selection.endIndex()));
                            }

                            graphics.textHighlight(drawX + drawBegin, drawTop, drawX + drawEnd, drawTop + 9, true);
                        }

                        drawTop += 9;
                    }
                }
            }

            if (this.isHovered()) {
                graphics.requestCursor(CursorTypes.IBEAM);
            }

            if (this.preeditOverlay != null) {
                this.preeditOverlay.updateInputPosition(cursorX, cursorY);
                graphics.setPreeditOverlay(this.preeditOverlay);
            }
        }
    }

    @Override
    protected void extractDecorations(GuiGraphicsExtractor graphics) {
        super.extractDecorations(graphics);
        if (this.textField.hasCharacterLimit()) {
            int characterLimit = this.textField.characterLimit();
            Component countText = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.getPlainText().length(), characterLimit);
            graphics.text(this.font, countText, this.getX() + this.width - this.font.width(countText), this.getY() + this.height + 4, -6250336);
        }
    }

    @Override
    public int getInnerHeight() {
        return 9 * this.textField.getLineCount();
    }

    private void scrollToCursor() {
        double scrollAmount = this.scrollAmount();
        StyledMultilineTextField.StringView firstFullyVisibleLine = this.textField.getLineView((int) (scrollAmount / 9.0));
        if (this.textField.cursor() <= firstFullyVisibleLine.beginIndex()) {
            scrollAmount = (double) (this.textField.getLineAtCursor() * 9);
        } else {
            StyledMultilineTextField.StringView lastFullyVisibleLine = this.textField.getLineView((int) ((scrollAmount + (double) this.height) / 9.0) - 1);
            if (this.textField.cursor() > lastFullyVisibleLine.endIndex()) {
                scrollAmount = (double) (this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding());
            }
        }

        this.setScrollAmount(scrollAmount);
    }

    private void seekCursorScreen(double x, double y) {
        double mouseX = x - (double) this.getX() - (double) this.innerPadding();
        double mouseY = y - (double) this.getY() - (double) this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(mouseX, mouseY);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }
        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    // ==================== 样式编辑方法 ====================

    public void setTextStyle() {
        if (!this.textField.hasSelection()) {
            return;
        }
        this.textField.applyStyleToSelection(
                Style.EMPTY
                        .withBold(boldButton.getValue())
                        .withItalic(italicButton.getValue())
                        .withUnderlined(underlineButton.getValue())
                        .withStrikethrough(strikethroughButton.getValue())
                        .withObfuscated(obfuscatedButton.getValue())
        );
        this.syncSelectionStyle();
        this.textBlockScreen.updateComponentLines(this.textField.getComponents());
    }

    public void setColorTextStyle() {
        if (!this.textField.hasSelection()) {
            return;
        }
        this.textField.applyStyleToSelection(
                Style.EMPTY
                        .withBold(boldButton.getValue())
                        .withItalic(italicButton.getValue())
                        .withUnderlined(underlineButton.getValue())
                        .withStrikethrough(strikethroughButton.getValue())
                        .withObfuscated(obfuscatedButton.getValue())
                        .withColor(colorWidget.getColor())
        );
        this.syncSelectionStyle();
        this.textBlockScreen.updateComponentLines(this.textField.getComponents());
    }

    public void setFontTextStyle() {
        if (!this.textField.hasSelection()) {
            return;
        }
        this.textField.applyStyleToSelection(
                Style.EMPTY
                        .withBold(boldButton.getValue())
                        .withItalic(italicButton.getValue())
                        .withUnderlined(underlineButton.getValue())
                        .withStrikethrough(strikethroughButton.getValue())
                        .withObfuscated(obfuscatedButton.getValue()),
                fontList.getSelectedFont()
        );
        this.syncSelectionStyle();
        this.textBlockScreen.updateComponentLines(this.textField.getComponents());
    }

    private void syncSelectionStyle() {
        if (!this.textField.hasSelection()) {
            return;
        }
        Style selectionStyle = this.getUniformSelectionStyle();
        if (selectionStyle != null) {
            this.boldButton.setValue(this.toBool(selectionStyle.isBold()));
            this.italicButton.setValue(this.toBool(selectionStyle.isItalic()));
            this.underlineButton.setValue(this.toBool(selectionStyle.isUnderlined()));
            this.strikethroughButton.setValue(this.toBool(selectionStyle.isStrikethrough()));
            this.obfuscatedButton.setValue(this.toBool(selectionStyle.isObfuscated()));
            if (selectionStyle.getColor() != null) {
                int value = selectionStyle.getColor().getValue();
                this.colorWidget.setColor(value);
                this.textBlockScreen.colorButton.color = value;
            }
        }
    }

    private Style getUniformSelectionStyle() {
        if (!this.textField.hasSelection()) {
            return null;
        }
        List<Component> components = this.textField.getComponents();
        int start = this.textField.getSelectionStart();
        int end = this.textField.getSelectionEnd();
        int currentIndex = 0;
        Style style = null;

        for (Component component : components) {
            String text = component.getString();
            int componentEnd = currentIndex + text.length();
            if (componentEnd <= start) {
                currentIndex = componentEnd;
                continue;
            }
            if (currentIndex >= end) {
                break;
            }

            if (!text.isEmpty()) {
                Style componentStyle = component.getStyle();
                if (style == null) {
                    style = componentStyle;
                } else if (!style.equals(componentStyle)) {
                    return null;
                }
            }
            currentIndex = componentEnd;
        }
        return style;
    }

    private boolean toBool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    public void clearStyle() {
        if (!this.textField.hasSelection()) {
            return;
        }
        this.textField.applyStyleToSelection(Style.EMPTY, Minecraft.DEFAULT_FONT);
        this.syncSelectionStyle();
        this.textBlockScreen.updateComponentLines(this.textField.getComponents());
    }

    // ==================== Builder 模式支持 ====================

    public static Builder builder(Font font) {
        return new Builder(font);
    }

    public static class Builder {
        private final Font font;
        private int x;
        private int y;
        private Component placeholder = Component.empty();
        private int textColor = -2039584;
        private boolean textShadow = true;
        private int cursorColor = -3092272;
        private boolean showBackground = true;
        private boolean showDecorations = true;
        private TextBlockScreen textBlockScreen;

        public Builder(Font font) {
            this.font = font;
        }

        public Builder setX(int x) {
            this.x = x;
            return this;
        }

        public Builder setY(int y) {
            this.y = y;
            return this;
        }

        public Builder setPlaceholder(Component placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setTextShadow(boolean textShadow) {
            this.textShadow = textShadow;
            return this;
        }

        public Builder setCursorColor(int cursorColor) {
            this.cursorColor = cursorColor;
            return this;
        }

        public Builder setShowBackground(boolean showBackground) {
            this.showBackground = showBackground;
            return this;
        }

        public Builder setShowDecorations(boolean showDecorations) {
            this.showDecorations = showDecorations;
            return this;
        }

        public Builder setTextBlockScreen(TextBlockScreen textBlockScreen) {
            this.textBlockScreen = textBlockScreen;
            return this;
        }

        public StyledMultiLineEditBox build(int width, int height, Component narration) {
            return new StyledMultiLineEditBox(
                    this.font, this.x, this.y, width, height, this.placeholder, narration,
                    this.textColor, this.textShadow, this.cursorColor, this.showBackground,
                    this.showDecorations, this.textBlockScreen
            );
        }
    }
}
