package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 建模工作台的操作说明界面。
 * <p>
 * 打开时压住 {@link ModelingScreen}，关闭（Esc / 返回按钮）后回到原界面，
 * 界面状态（相机、选中元素、未提交的改动）都不会丢。
 */
public class ModelingHelpScreen extends Screen {

    private final Screen parent;

    private static final int PANEL_WIDTH = 300;
    private static final int PADDING = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int SECTION_GAP = 6;
    private static final int BOTTOM_BUTTON = 20;

    private static final int PANEL_BG = 0xF0181818;
    private static final int PANEL_BORDER = 0xFF0B0B0B;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int SECTION_COLOR = 0xFF8FD7B0;
    private static final int KEY_COLOR = 0xFFE6C07B;
    private static final int TEXT_COLOR = 0xFFC6C6C6;
    private static final int BUTTON_BG = 0xFF2F2F2F;
    private static final int BUTTON_BG_HOVER = 0xFF3F3F3F;
    private static final int BUTTON_TEXT = 0xFFDDDDDD;
    private static final int BUTTON_TEXT_HOVER = 0xFFFFFFFF;

    /** 一段内容：标题 + 条目。 */
    private record Section(String titleKey, List<Entry> entries) {
    }

    /** 一条说明：按键 + 描述。 */
    private record Entry(String keyLabel, String textKey) {
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("gui.yuushya.modeling.help.sec.camera", List.of(
                    new Entry("中键拖动", "gui.yuushya.modeling.help.camera.orbit"),
                    new Entry("右键拖动空白", "gui.yuushya.modeling.help.camera.pan"),
                    new Entry("滚轮", "gui.yuushya.modeling.help.camera.zoom"),
                    new Entry("点击导航轴端点", "gui.yuushya.modeling.help.camera.look"))),
            new Section("gui.yuushya.modeling.help.sec.transform", List.of(
                    new Entry("左键拖轴", "gui.yuushya.modeling.help.transform.move"),
                    new Entry("右键拖轴", "gui.yuushya.modeling.help.transform.rotate"),
                    new Entry("Shift + 拖动", "gui.yuushya.modeling.help.transform.fine"),
                    new Entry("拖动右侧数字", "gui.yuushya.modeling.help.transform.scrub"),
                    new Entry("单击数字再输入", "gui.yuushya.modeling.help.transform.type"))),
            new Section("gui.yuushya.modeling.help.sec.element", List.of(
                    new Entry("Ctrl + N", "gui.yuushya.modeling.help.element.add"),
                    new Entry("Ctrl + D", "gui.yuushya.modeling.help.element.duplicate"),
                    new Entry("Delete", "gui.yuushya.modeling.help.element.remove"),
                    new Entry("Shift + ↑ / ↓", "gui.yuushya.modeling.help.element.order"),
                    new Entry("👁 按钮", "gui.yuushya.modeling.help.element.shown"),
                    new Entry("右键元素行", "gui.yuushya.modeling.help.element.menu"))),
            new Section("gui.yuushya.modeling.help.sec.file", List.of(
                    new Entry("Ctrl + C", "gui.yuushya.modeling.help.file.copy"),
                    new Entry("Ctrl + V", "gui.yuushya.modeling.help.file.paste"),
                    new Entry("Ctrl + S", "gui.yuushya.modeling.help.file.save"),
                    new Entry("📊 按钮", "gui.yuushya.modeling.help.file.stats"))),
            new Section("gui.yuushya.modeling.help.sec.other", List.of(
                    new Entry("右上 < >", "gui.yuushya.modeling.help.other.property"),
                    new Entry("形状按钮", "gui.yuushya.modeling.help.other.shape"),
                                new Entry("W / L 按钮", "gui.yuushya.modeling.help.other.axis"),
                    new Entry("▦ 按钮", "gui.yuushya.modeling.help.other.collision"),
                    new Entry("Esc", "gui.yuushya.modeling.help.other.esc"))));

    public ModelingHelpScreen(Screen parent) {
        super(Component.translatable("gui.yuushya.modeling.help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 全部自绘，不注册原版控件
    }

    private int panelHeight() {
        return Math.min(this.height - 24, contentHeight(this.font) + PADDING * 2);
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(8, (this.height - panelHeight()) / 2);
    }

    /** 返回按钮的矩形：面板内底部居中。 */
    private ModelingLayout.Rect backButton() {
        int panelHeight = panelHeight();
        int top = panelTop();
        int y = top + panelHeight - BOTTOM_BUTTON - PADDING / 2;
        return new ModelingLayout.Rect(this.width / 2 - 50, y, 100, BOTTOM_BUTTON);
    }

    /** 帮助界面不暂停游戏，也不画世界背景。 */
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0000000);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelHeight = panelHeight();
        int left = panelLeft();
        int top = panelTop();

        guiGraphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + panelHeight + 1, PANEL_BORDER);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight, PANEL_BG);

        Component title = Component.translatable("gui.yuushya.modeling.help.title");
        guiGraphics.drawString(this.font, title, left + (PANEL_WIDTH - this.font.width(title)) / 2,
                top + PADDING / 2 + 1, TITLE_COLOR, false);

        // 底部留出按钮高度，避免文字压到按钮上
        int contentAvailable = panelHeight - PADDING * 2 - 12 - BOTTOM_BUTTON - 4;
        renderSections(guiGraphics, left + PADDING, top + PADDING + 12, contentAvailable);
        renderBackButton(guiGraphics, mouseX, mouseY);
    }

    /** 自绘返回按钮：深色方块 + 居中文字，悬停时提亮。 */
    private void renderBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ModelingLayout.Rect rect = backButton();
        boolean hovered = rect.contains(mouseX, mouseY);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, PANEL_BORDER);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), PANEL_BORDER);

        Component label = Component.translatable("gui.yuushya.modeling.help.back");
        guiGraphics.drawString(this.font, label,
                rect.x() + (rect.width() - this.font.width(label)) / 2,
                rect.y() + (rect.height() - 8) / 2,
                hovered ? BUTTON_TEXT_HOVER : BUTTON_TEXT, false);
    }

    private int contentHeight(Font font) {
        int height = font.lineHeight; // 标题
        for (Section section : SECTIONS) {
            height += LINE_HEIGHT;                       // 分组标题
            height += section.entries().size() * LINE_HEIGHT;
            height += SECTION_GAP;
        }
        height += BOTTOM_BUTTON + 4;                     // 底部返回按钮
        return height;
    }

    private void renderSections(GuiGraphics guiGraphics, int x, int y, int available) {
        int cursor = y;
        int limit = y + available;
        for (Section section : SECTIONS) {
            if (cursor + LINE_HEIGHT > limit) {
                return;
            }
            guiGraphics.drawString(this.font, Component.translatable(section.titleKey()),
                    x, cursor, SECTION_COLOR, false);
            cursor += LINE_HEIGHT;

            for (Entry entry : section.entries()) {
                if (cursor + LINE_HEIGHT > limit) {
                    return;
                }
                guiGraphics.drawString(this.font, entry.keyLabel(), x, cursor, KEY_COLOR, false);
                Component text = Component.translatable(entry.textKey());
                guiGraphics.drawString(this.font, text, x + 86, cursor, TEXT_COLOR, false);
                cursor += LINE_HEIGHT;
            }
            cursor += SECTION_GAP;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && backButton().contains(mouseX, mouseY)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 供界面判断是否需要绘制滚动条等扩展（当前内容固定，不需要）。 */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Section section : SECTIONS) {
            keys.add(section.titleKey());
            for (Entry entry : section.entries()) {
                keys.add(entry.textKey());
            }
        }
        return keys;
    }
}
