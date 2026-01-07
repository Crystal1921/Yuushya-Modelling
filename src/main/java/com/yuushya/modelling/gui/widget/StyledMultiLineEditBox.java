package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class StyledMultiLineEditBox extends AbstractScrollWidget {
    private static final int CURSOR_INSERT_WIDTH = 1;
    private static final int CURSOR_INSERT_COLOR = -3092272;
    private static final String CURSOR_APPEND_CHARACTER = "_";
    private static final int TEXT_COLOR = -2039584;
    private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;

    private final Font font;
    private final Component placeholder;
    private final StyledMultilineTextField textField;
    private final CycleButton<Boolean> boldButton;
    private final CycleButton<Boolean> italicButton;
    private final CycleButton<Boolean> underlineButton;
    private final CycleButton<Boolean> strikethroughButton;
    private final CycleButton<Boolean> obfuscatedButton;
    private final ColorWidget colorWidget;
    private final TextBlockScreen textBlockScreen;
    private long focusedTime = Util.getMillis();

    public StyledMultiLineEditBox(Font font, int x, int y, int width, int height, Component placeholder, Component message, TextBlockScreen textBlockScreen) {
        super(x, y, width, height, message);
        this.font = font;
        this.placeholder = placeholder;
        this.textField = new StyledMultilineTextField(font, width - this.totalInnerPadding(), textBlockScreen);
        this.textField.setCursorListener(() -> {
            this.scrollToCursor();
            this.syncSelectionStyle();
        });
        this.boldButton = textBlockScreen.boldButton;
        this.italicButton = textBlockScreen.italicButton;
        this.underlineButton = textBlockScreen.underlineButton;
        this.strikethroughButton = textBlockScreen.strikethroughButton;
        this.obfuscatedButton = textBlockScreen.obfuscatedButton;
        this.colorWidget = textBlockScreen.colorWidget;
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
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(
                NarratedElementType.TITLE,
                Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue())
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.textField.setSelecting(Screen.hasShiftDown());
            this.seekCursorScreen(mouseX, mouseY);
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        } else if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.textField.setSelecting(true);
            this.seekCursorScreen(mouseX, mouseY);
            this.textField.setSelecting(Screen.hasShiftDown());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.textField.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(codePoint)) {
            String string = Character.toString(codePoint);
            Component component = Component.literal(string)
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
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String plainText = this.textField.getPlainText();
        List<Component> components = this.textField.getComponents();

        if (plainText.isEmpty() && !this.isFocused()) {
            // 渲染占位符
            guiGraphics.drawWordWrap(
                    this.font,
                    this.placeholder,
                    this.getX() + this.innerPadding(),
                    this.getY() + this.innerPadding(),
                    this.width - this.totalInnerPadding(),
                    PLACEHOLDER_TEXT_COLOR
            );
        } else {
            int cursor = this.textField.cursor();
            boolean shouldShowCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / CURSOR_BLINK_INTERVAL_MS % 2L == 0L;
            boolean cursorInText = cursor < plainText.length();

            int cursorX = 0;
            int cursorY = 0;
            int lastLineEndX = 0;
            int lastLineY = 0;
            int currentY = this.getY() + this.innerPadding();
            boolean cursorFound = false;

            // 先渲染所有文本，同时记录光标位置
            for (StyledMultilineTextField.StringView lineView : this.textField.iterateLines()) {
                boolean isLineVisible = this.withinContentAreaTopBottom(currentY, currentY + 9);

                if (isLineVisible) {
                    // 完整渲染整行文本（不分割）
                    int lineEndX = this.renderStyledTextSegment(
                            guiGraphics,
                            components,
                            plainText,
                            lineView.beginIndex(),
                            lineView.endIndex(),
                            this.getX() + this.innerPadding(),
                            currentY
                    ) - 1;

                    lastLineEndX = lineEndX;
                    lastLineY = currentY;

                    // 如果光标在当前行，计算光标的 X 坐标
                    if (!cursorFound && cursor >= lineView.beginIndex() && cursor <= lineView.endIndex()) {
                        if (cursor == lineView.endIndex() && cursor < plainText.length() && plainText.charAt(cursor) == '\n') {
                            // 光标在行尾且下一个字符是换行符
                            cursorX = lineEndX;
                        } else {
                            // 计算光标前的文本宽度
                            String textBeforeCursor = plainText.substring(lineView.beginIndex(), cursor);
                            cursorX = this.getX() + this.innerPadding() + this.font.width(textBeforeCursor);
                        }
                        cursorY = currentY;
                        cursorFound = true;
                    }
                }

                currentY += 9;
            }

            // 渲染光标（在文本之后，作为覆盖层）
            if (shouldShowCursor) {
                if (cursorInText && cursorFound) {
                    // 光标在文本中间
                    guiGraphics.fill(cursorX, cursorY - 1, cursorX + CURSOR_INSERT_WIDTH, cursorY + 10, CURSOR_INSERT_COLOR);
                } else if (!cursorInText && this.withinContentAreaTopBottom(lastLineY, lastLineY + 9)) {
                    // 光标在文本末尾
                    guiGraphics.drawString(this.font, CURSOR_APPEND_CHARACTER, lastLineEndX, lastLineY, CURSOR_INSERT_COLOR);
                }
            }

            // 渲染选择高亮（在光标之后，确保高亮在最上层）
            if (this.textField.hasSelection()) {
                this.renderSelection(guiGraphics, plainText);
            }
        }
    }

    /**
     * 渲染带样式的文本段
     */
    private int renderStyledTextSegment(
            GuiGraphics guiGraphics,
            List<Component> components,
            String plainText,
            int startIndex,
            int endIndex,
            int x,
            int y
    ) {
        if (startIndex >= endIndex) {
            return x;
        }

        int currentX = x;
        int currentIndex = 0;

        // 遍历所有 Component，找到需要渲染的部分
        for (Component component : components) {
            String componentText = component.getString();
            int componentLength = componentText.length();
            int componentEnd = currentIndex + componentLength;

            // 如果当前 Component 在渲染范围之前，跳过
            if (componentEnd <= startIndex) {
                currentIndex = componentEnd;
                continue;
            }

            // 如果当前 Component 在渲染范围之后，停止
            if (currentIndex >= endIndex) {
                break;
            }

            // 计算需要渲染的子串
            int renderStart = Math.max(0, startIndex - currentIndex);
            int renderEnd = Math.min(componentLength, endIndex - currentIndex);
            String renderText = componentText.substring(renderStart, renderEnd);

            // 渲染带样式的文本
            if (!renderText.isEmpty()) {
                Component styledSegment = Component.literal(renderText).setStyle(component.getStyle());
                currentX = guiGraphics.drawString(this.font, styledSegment, currentX, y, TEXT_COLOR);
            }

            currentIndex = componentEnd;
        }

        return currentX;
    }

    /**
     * 渲染选择高亮
     */
    private void renderSelection(GuiGraphics guiGraphics, String plainText) {
        StyledMultilineTextField.StringView selection = this.textField.getSelected();
        int baseX = this.getX() + this.innerPadding();
        int currentY = this.getY() + this.innerPadding();

        for (StyledMultilineTextField.StringView lineView : this.textField.iterateLines()) {
            // 如果选择区域在当前行之后，移动到下一行
            if (selection.beginIndex() > lineView.endIndex()) {
                currentY += 9;
                continue;
            }

            // 如果选择区域在当前行之前，停止
            if (lineView.beginIndex() > selection.endIndex()) {
                break;
            }

            // 渲染当前行的高亮
            if (this.withinContentAreaTopBottom(currentY, currentY + 9)) {
                int highlightStart = Math.max(selection.beginIndex(), lineView.beginIndex());
                int highlightEnd = Math.min(selection.endIndex(), lineView.endIndex());

                // 计算高亮起始位置
                int startX = baseX + this.font.width(
                        plainText.substring(lineView.beginIndex(), highlightStart)
                );

                // 计算高亮结束位置
                int endX;
                if (selection.endIndex() > lineView.endIndex()) {
                    endX = this.width - this.innerPadding();
                } else {
                    endX = baseX + this.font.width(
                            plainText.substring(lineView.beginIndex(), highlightEnd)
                    );
                }

                this.renderHighlight(guiGraphics, startX, currentY, endX, currentY + 9);
            }

            currentY += 9;
        }
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
        super.renderDecorations(guiGraphics);
        if (this.textField.hasCharacterLimit()) {
            int limit = this.textField.characterLimit();
            Component counterText = Component.translatable(
                    "gui.multiLineEditBox.character_limit",
                    this.textField.getPlainText().length(),
                    limit
            );
            guiGraphics.drawString(
                    this.font,
                    counterText,
                    this.getX() + this.width - this.font.width(counterText),
                    this.getY() + this.height + 4,
                    10526880
            );
        }
    }

    @Override
    public int getInnerHeight() {
        return 9 * this.textField.getLineCount();
    }

    @Override
    protected boolean scrollbarVisible() {
        return (double) this.textField.getLineCount() > this.getDisplayableLineCount();
    }

    @Override
    protected double scrollRate() {
        return 9.0 / 2.0;
    }

    private void renderHighlight(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
        guiGraphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, -16776961);
    }

    private void scrollToCursor() {
        double scrollAmount = this.scrollAmount();
        StyledMultilineTextField.StringView topLine = this.textField.getLineView((int) (scrollAmount / 9.0));

        if (this.textField.cursor() <= topLine.beginIndex()) {
            scrollAmount = this.textField.getLineAtCursor() * 9;
        } else {
            StyledMultilineTextField.StringView bottomLine = this.textField.getLineView(
                    (int) ((scrollAmount + (double) this.height) / 9.0) - 1
            );
            if (this.textField.cursor() > bottomLine.endIndex()) {
                scrollAmount = this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding();
            }
        }

        this.setScrollAmount(scrollAmount);
    }

    private double getDisplayableLineCount() {
        return (double) (this.height - this.totalInnerPadding()) / 9.0;
    }

    private void seekCursorScreen(double mouseX, double mouseY) {
        double relativeX = mouseX - (double) this.getX() - (double) this.innerPadding();
        double relativeY = mouseY - (double) this.getY() - (double) this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(relativeX, relativeY);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }
    }

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
        this.textField.applyStyleToSelection(Style.EMPTY);
        this.syncSelectionStyle();
        this.textBlockScreen.updateComponentLines(this.textField.getComponents());
    }
}
