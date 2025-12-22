package com.yuushya.modelling.gui.widget;

import com.google.common.annotations. VisibleForTesting;
import com.google.common.collect.Lists;
import net.minecraft.client. Minecraft;
import net.minecraft. client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util. Mth;
import net.minecraft. util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker. OnlyIn;

import java.util. ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class StyledMultilineTextField {
    public static final int NO_CHARACTER_LIMIT = Integer.MAX_VALUE;
    private static final int LINE_SEEK_PIXEL_BIAS = 2;

    private final Font font;
    private final List<StyledMultilineTextField.StringView> displayLines = Lists.newArrayList();

    // 核心：使用 List<Component> 替代 String
    private final List<Component> components;
    private MutableComponent combinedText;
    private String plainText; // 缓存的纯文本，用于光标位置计算

    private int cursor;
    private int selectCursor;
    private boolean selecting;
    private int characterLimit = Integer.MAX_VALUE;
    private final int width;

    private Consumer<List<Component>> valueListener = components -> {};
    private Runnable cursorListener = () -> {};

    public StyledMultilineTextField(Font font, int width) {
        this.font = font;
        this.width = width;
        this.components = new ArrayList<>();
        this.combinedText = Component.empty();
        this.plainText = "";
        this.setValue(new ArrayList<>());
    }

    // ==================== 配置方法 ====================

    public int characterLimit() {
        return this.characterLimit;
    }

    public void setCharacterLimit(int characterLimit) {
        if (characterLimit < 0) {
            throw new IllegalArgumentException("Character limit cannot be negative");
        }
        this.characterLimit = characterLimit;
    }

    public boolean hasCharacterLimit() {
        return this.characterLimit != Integer.MAX_VALUE;
    }

    public void setValueListener(Consumer<List<Component>> valueListener) {
        this.valueListener = valueListener;
    }

    public void setCursorListener(Runnable cursorListener) {
        this.cursorListener = cursorListener;
    }

    // ==================== 值操作 ====================

    public void setValue(List<Component> components) {
        this.components. clear();
        this.components. addAll(components);
        rebuildCombinedText();

        this.cursor = this.plainText.length();
        this.selectCursor = this.cursor;
        this.onValueChange();
    }

    public List<Component> getComponents() {
        return new ArrayList<>(this.components);
    }

    public String getPlainText() {
        return this.plainText;
    }

    // ==================== 文本编辑 ====================

    public void insertText(String text) {
        insertStyledText(Component.literal(text));
    }

    public void insertStyledText(Component component) {
        if (component. getString().isEmpty() && ! this.hasSelection()) {
            return;
        }

        String insertText = StringUtil.filterText(component.getString(), true);
        if (this.hasCharacterLimit()) {
            int availableSpace = this.characterLimit - this.plainText.length()
                    + getSelectedText().length();
            if (insertText.length() > availableSpace) {
                insertText = insertText.substring(0, availableSpace);
            }
        }

        StringView selection = this.getSelected();

        // 构建新的 Component 列表
        List<Component> newComponents = new ArrayList<>();

        // 分割逻辑：找到选择范围对应的 Component
        int currentIndex = 0;
        boolean inserted = false;

        for (Component comp : this.components) {
            String compText = comp.getString();
            int compLength = compText.length();
            int compEnd = currentIndex + compLength;

            // 如果当前 Component 在选择范围之前
            if (compEnd <= selection.beginIndex) {
                newComponents.add(comp);
                currentIndex = compEnd;
                continue;
            }

            // 如果当前 Component 在选择范围之后
            if (currentIndex >= selection.endIndex) {
                newComponents.add(comp);
                currentIndex = compEnd;
                continue;
            }

            // 当前 Component 与选择范围有交集
            if (! inserted) {
                // 添加选择前的部分
                if (currentIndex < selection.beginIndex) {
                    int beforeLen = selection.beginIndex - currentIndex;
                    String beforeText = compText.substring(0, beforeLen);
                    newComponents.add(Component.literal(beforeText).setStyle(comp.getStyle()));
                }

                // 插入新文本（保留原有样式或使用新 Component 的样式）
                if (! insertText.isEmpty()) {
                    newComponents.add(Component. literal(insertText).setStyle(component.getStyle()));
                }

                // 添加选择后的部分
                if (compEnd > selection.endIndex) {
                    int afterStart = selection.endIndex - currentIndex;
                    String afterText = compText.substring(afterStart);
                    newComponents. add(Component.literal(afterText).setStyle(comp.getStyle()));
                }

                inserted = true;
            } else {
                // 已插入，处理选择范围后的部分
                if (compEnd > selection.endIndex) {
                    int afterStart = selection.endIndex - currentIndex;
                    String afterText = compText.substring(afterStart);
                    newComponents.add(Component. literal(afterText).setStyle(comp.getStyle()));
                }
            }

            currentIndex = compEnd;
        }

        // 如果在末尾插入
        if (!inserted && ! insertText.isEmpty()) {
            newComponents.add(Component.literal(insertText).setStyle(component. getStyle()));
        }

        this.components. clear();
        this.components.addAll(newComponents);

        this.cursor = selection.beginIndex + insertText.length();
        this.selectCursor = this.cursor;

        rebuildCombinedText();
        this.onValueChange();
    }

    public void deleteText(int length) {
        if (!this.hasSelection()) {
            this.selectCursor = Mth.clamp(this. cursor + length, 0, this.plainText.length());
        }
        this.insertText("");
    }

    // ==================== 光标操作 ====================

    public int cursor() {
        return this.cursor;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    public StringView getSelected() {
        return new StringView(
                Math.min(this.selectCursor, this.cursor),
                Math.max(this.selectCursor, this.cursor)
        );
    }

    public void seekCursor(Whence whence, int position) {
        switch (whence) {
            case ABSOLUTE:
                this.cursor = position;
                break;
            case RELATIVE:
                this.cursor += position;
                break;
            case END:
                this.cursor = this.plainText.length() + position;
        }

        this.cursor = Mth.clamp(this.cursor, 0, this.plainText.length());
        this.cursorListener.run();

        if (! this.selecting) {
            this.selectCursor = this.cursor;
        }
    }

    public void seekCursorLine(int offset) {
        if (offset != 0) {
            StringView currentLine = this.getCursorLineView();
            int pixelPos = this.font.width(this.plainText.substring(currentLine.beginIndex, this.cursor)) + LINE_SEEK_PIXEL_BIAS;

            StringView targetLine = this.getCursorLineView(offset);
            int charOffset = this.font.plainSubstrByWidth(
                    this.plainText.substring(targetLine.beginIndex, targetLine.endIndex),
                    pixelPos
            ).length();

            this. seekCursor(Whence.ABSOLUTE, targetLine.beginIndex + charOffset);
        }
    }

    public void seekCursorToPoint(double x, double y) {
        int lineIndex = Mth.clamp(Mth.floor(y / 9.0), 0, this.displayLines.size() - 1);
        StringView line = this.displayLines. get(lineIndex);

        int charOffset = this.font.plainSubstrByWidth(
                this. plainText.substring(line.beginIndex, line.endIndex),
                Mth.floor(x)
        ).length();

        this.seekCursor(Whence.ABSOLUTE, line. beginIndex + charOffset);
    }

    // ==================== 行操作 ====================

    public int getLineCount() {
        return this.displayLines.size();
    }

    public int getLineAtCursor() {
        for (int i = 0; i < this.displayLines.size(); i++) {
            StringView view = this.displayLines.get(i);
            if (this.cursor >= view.beginIndex && this.cursor <= view.endIndex) {
                return i;
            }
        }
        return -1;
    }

    public StringView getLineView(int lineNumber) {
        return this.displayLines.get(Mth.clamp(lineNumber, 0, this.displayLines.size() - 1));
    }

    public Iterable<StringView> iterateLines() {
        return this. displayLines;
    }

    private StringView getCursorLineView() {
        return this.getCursorLineView(0);
    }

    private StringView getCursorLineView(int offset) {
        int lineIndex = this.getLineAtCursor();
        if (lineIndex < 0) {
            throw new IllegalStateException("Cursor is not within text (cursor = " + this.cursor + ", length = " + this.plainText. length() + ")");
        }
        return this.displayLines.get(Mth.clamp(lineIndex + offset, 0, this.displayLines.size() - 1));
    }

    // ==================== 键盘处理 ====================

    public boolean keyPressed(int keyCode) {
        this.selecting = Screen.hasShiftDown();

        if (Screen.isSelectAll(keyCode)) {
            this. cursor = this.plainText.length();
            this.selectCursor = 0;
            return true;
        } else if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            return true;
        } else if (Screen.isPaste(keyCode)) {
            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        } else if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            this.insertText("");
            return true;
        }

        switch (keyCode) {
            case 257: // Enter
            case 335: // Numpad Enter
                this.insertText("\n");
                return true;
            case 259: // Backspace
                if (Screen.hasControlDown()) {
                    StringView word = this.getPreviousWord();
                    this.deleteText(word.beginIndex - this.cursor);
                } else {
                    this.deleteText(-1);
                }
                return true;
            case 261: // Delete
                if (Screen.hasControlDown()) {
                    StringView word = this.getNextWord();
                    this.deleteText(word.beginIndex - this.cursor);
                } else {
                    this. deleteText(1);
                }
                return true;
            case 262: // Right Arrow
                if (Screen.hasControlDown()) {
                    StringView word = this.getNextWord();
                    this.seekCursor(Whence. ABSOLUTE, word.beginIndex);
                } else {
                    this.seekCursor(Whence.RELATIVE, 1);
                }
                return true;
            case 263: // Left Arrow
                if (Screen.hasControlDown()) {
                    StringView word = this.getPreviousWord();
                    this.seekCursor(Whence. ABSOLUTE, word.beginIndex);
                } else {
                    this.seekCursor(Whence.RELATIVE, -1);
                }
                return true;
            case 264: // Down Arrow
                if (! Screen.hasControlDown()) {
                    this.seekCursorLine(1);
                }
                return true;
            case 265: // Up Arrow
                if (!Screen.hasControlDown()) {
                    this.seekCursorLine(-1);
                }
                return true;
            case 266: // Page Up
                this.seekCursor(Whence.ABSOLUTE, 0);
                return true;
            case 267: // Page Down
                this.seekCursor(Whence.END, 0);
                return true;
            case 268: // Home
                if (Screen.hasControlDown()) {
                    this.seekCursor(Whence.ABSOLUTE, 0);
                } else {
                    this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
                }
                return true;
            case 269: // End
                if (Screen.hasControlDown()) {
                    this.seekCursor(Whence.END, 0);
                } else {
                    this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
                }
                return true;
            default:
                return false;
        }
    }

    // ==================== 工具方法 ====================

    public boolean hasSelection() {
        return this. selectCursor != this.cursor;
    }

    @VisibleForTesting
    public String getSelectedText() {
        StringView selection = this.getSelected();
        return this.plainText.substring(selection. beginIndex, selection.endIndex);
    }

    @VisibleForTesting
    public StringView getPreviousWord() {
        if (this.plainText.isEmpty()) {
            return StringView. EMPTY;
        }

        int i = Mth.clamp(this. cursor, 0, this.plainText. length() - 1);

        while (i > 0 && Character.isWhitespace(this.plainText. charAt(i - 1))) {
            i--;
        }

        while (i > 0 && ! Character.isWhitespace(this.plainText.charAt(i - 1))) {
            i--;
        }

        return new StringView(i, this.getWordEndPosition(i));
    }

    @VisibleForTesting
    public StringView getNextWord() {
        if (this.plainText.isEmpty()) {
            return StringView.EMPTY;
        }

        int i = Mth.clamp(this.cursor, 0, this. plainText.length() - 1);

        while (i < this.plainText.length() && ! Character.isWhitespace(this.plainText.charAt(i))) {
            i++;
        }

        while (i < this.plainText.length() && Character.isWhitespace(this.plainText.charAt(i))) {
            i++;
        }

        return new StringView(i, this.getWordEndPosition(i));
    }

    private int getWordEndPosition(int start) {
        int i = start;
        while (i < this.plainText.length() && !Character.isWhitespace(this.plainText.charAt(i))) {
            i++;
        }
        return i;
    }

    // ==================== 内部方法 ====================

    private void rebuildCombinedText() {
        this.combinedText = Component.empty();
        for (Component component : this.components) {
            this.combinedText. append(component);
        }
        this.plainText = this. combinedText.getString();

        // 应用字符限制
        if (this.hasCharacterLimit() && this.plainText.length() > this.characterLimit) {
            List<Component> truncated = new ArrayList<>();
            int count = 0;

            for (Component comp : this.components) {
                String text = comp.getString();
                if (count + text.length() <= this.characterLimit) {
                    truncated.add(comp);
                    count += text.length();
                } else {
                    int remaining = this.characterLimit - count;
                    if (remaining > 0) {
                        truncated.add(Component.literal(text.substring(0, remaining)).setStyle(comp.getStyle()));
                    }
                    break;
                }
            }

            this.components. clear();
            this.components. addAll(truncated);

            this.combinedText = Component.empty();
            for (Component comp : this.components) {
                this.combinedText.append(comp);
            }
            this.plainText = this.combinedText.getString();
        }
    }

    private void onValueChange() {
        this.reflowDisplayLines();
        this.valueListener.accept(this.getComponents());
        this.cursorListener.run();
    }

    private void reflowDisplayLines() {
        this.displayLines. clear();

        if (this.plainText. isEmpty()) {
            this.displayLines.add(StringView.EMPTY);
        } else {
            // 使用 String 版本的 splitLines 来获取索引
            this.font.getSplitter().splitLines(
                    this.plainText,     // 使用纯文本
                    this.width,
                    Style.EMPTY,
                    false,
                    (style, start, end) -> this.displayLines.add(new StringView(start, end))
            );

            // 处理末尾的换行符
            if (this. plainText.charAt(this.plainText.length() - 1) == '\n') {
                this.displayLines. add(new StringView(this.plainText. length(), this.plainText.length()));
            }
        }
    }

    // ==================== 辅助类 ====================

    public enum Whence {
        ABSOLUTE,
        RELATIVE,
        END
    }

    @OnlyIn(Dist.CLIENT)
    public static record StringView(int beginIndex, int endIndex) {
        static final StringView EMPTY = new StringView(0, 0);
    }
}