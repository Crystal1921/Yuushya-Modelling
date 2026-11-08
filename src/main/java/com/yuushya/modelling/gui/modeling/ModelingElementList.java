package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * 建模界面左侧的紧凑元素列表。
 * <p>
 * 不使用 {@code ObjectSelectionList}（它自带半透明底、45px 行高与额外边距），
 * 这里自绘 20px 行：图标 + 序号 + 名称，选中高亮，悬停高亮，滚轮滚动。
 */
public final class ModelingElementList {

    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int ROW_BG = 0x00000000;
    private static final int ROW_HOVER = 0xFF2C2C2C;
    private static final int ROW_SELECTED = 0xFF2B5C8A;
    private static final int TEXT = 0xFFDDDDDD;
    private static final int TEXT_SELECTED = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFF8A8A8A;

    /** 底部工具条高度。 */
    public static final int TOOLBAR_HEIGHT = 22;
    private static final int TOOL_SIZE = 18;
    private static final int TOOL_GAP = 2;
    private static final int TOOL_BG = 0xFF2A2A2A;
    private static final int TOOL_BG_HOVER = 0xFF3C3C3C;
    private static final int TOOL_BORDER = 0xFF0B0B0B;
    private static final int TOOL_TEXT = 0xFFCCCCCC;
    private static final int TOOL_TEXT_ACTIVE = 0xFFFFFFFF;

    /** 工具条上的四个动作。 */
    public enum Tool {
        ADD("+", "gui.yuushya.modeling.tool.add"),
        DUPLICATE("⧉", "gui.yuushya.modeling.tool.duplicate"),
        REMOVE("🗑", "gui.yuushya.modeling.tool.remove"),
        TOGGLE_SHOWN("👁", "gui.yuushya.modeling.tool.shown");

        final String symbol;
        final String tooltipKey;

        Tool(String symbol, String tooltipKey) {
            this.symbol = symbol;
            this.tooltipKey = tooltipKey;
        }
    }

    /** 工具条动作回调。 */
    public interface ToolListener {
        void onTool(Tool tool);
    }

    /** 列表行上的右键菜单（-1 = 未打开）。 */
    private int menuRow = -1;
    private int menuX;
    private int menuY;
    private ToolListener toolListener = tool -> {
    };
    private Tool hoveredTool;

    public void setToolListener(ToolListener listener) {
        this.toolListener = listener;
    }

    private final Minecraft minecraft;
    private final Font font;
    private final List<TransformBlockData> data;
    private final IntConsumer onSelect;

    @Setter
    private ModelingLayout.Rect rect = new ModelingLayout.Rect(0, 0, 0, 0);
    private int selected;
    private int scroll;

    public ModelingElementList(Minecraft minecraft, Font font, List<TransformBlockData> data,
                               int selected, IntConsumer onSelect) {
        this.minecraft = minecraft;
        this.font = font;
        this.data = data;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        scrollIntoView(selected);
    }

    private int visibleRows() {
        return Math.max(1, (this.rect.height() - TOOLBAR_HEIGHT) / ROW_HEIGHT);
    }

    /** 列表行区域（不含底部工具条）。 */
    private int rowsBottom() {
        return this.rect.bottom() - TOOLBAR_HEIGHT;
    }

    private int toolX(int index) {
        return this.rect.x() + 3 + index * (TOOL_SIZE + TOOL_GAP);
    }

    private int toolY() {
        return rowsBottom() + (TOOLBAR_HEIGHT - TOOL_SIZE) / 2;
    }

    /** 命中工具条按钮。 */
    private Tool toolAt(double mouseX, double mouseY) {
        if (mouseY < rowsBottom() || mouseY > this.rect.bottom()) {
            return null;
        }
        Tool[] tools = Tool.values();
        for (int i = 0; i < tools.length; i++) {
            int x = toolX(i);
            if (mouseX >= x && mouseX < x + TOOL_SIZE
                    && mouseY >= toolY() && mouseY < toolY() + TOOL_SIZE) {
                return tools[i];
            }
        }
        return null;
    }

    private int maxScroll() {
        return Math.max(0, this.data.size() - visibleRows());
    }

    private void scrollIntoView(int index) {
        if (index < this.scroll) {
            this.scroll = index;
        } else if (index >= this.scroll + visibleRows()) {
            this.scroll = index - visibleRows() + 1;
        }
        this.scroll = Math.clamp(this.scroll, 0, maxScroll());
    }

    // ---- 交互 ----

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.rect.contains(mouseX, mouseY);
    }

    /** 命中列表行号；不在行上返回 -1。 */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < this.rect.x() || mouseX >= this.rect.right()
                || mouseY < this.rect.y() || mouseY >= rowsBottom()) {
            return -1;
        }
        int row = this.scroll + (int) ((mouseY - this.rect.y()) / ROW_HEIGHT);
        return row >= 0 && row < this.data.size() ? row : -1;
    }

    /** @return 是否消费了本次点击 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 右键菜单打开时优先处理
        if (this.menuRow >= 0) {
            Tool menuTool = menuToolAt(mouseX, mouseY);
            int row = this.menuRow;
            this.menuRow = -1;
            if (menuTool != null) {
                this.selected = row;
                this.onSelect.accept(row);
                this.toolListener.onTool(menuTool);
            }
            return true;
        }
        // 工具条
        Tool tool = toolAt(mouseX, mouseY);
        if (tool != null) {
            this.toolListener.onTool(tool);
            return true;
        }
        // 列表行
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && row != this.selected) {
            this.selected = row;
            this.onSelect.accept(row);
        }
        return true;
    }

    /** 行上右键：打开上下文菜单。 */
    public boolean mouseRightClicked(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY) || toolAt(mouseX, mouseY) != null) {
            return false;
        }
        int row = rowAt(mouseX, mouseY);
        if (row < 0) {
            return false;
        }
        this.menuRow = row;
        this.menuX = (int) mouseX;
        this.menuY = (int) mouseY;
        return true;
    }

    public boolean isMenuOpen() {
        return this.menuRow >= 0;
    }

    public void closeMenu() {
        this.menuRow = -1;
    }

    private static final int MENU_WIDTH = 62;
    private static final int MENU_ITEM_HEIGHT = 16;
    private static final Tool[] MENU_TOOLS = {Tool.DUPLICATE, Tool.REMOVE, Tool.TOGGLE_SHOWN};

    private Tool menuToolAt(double mouseX, double mouseY) {
        if (this.menuRow < 0) {
            return null;
        }
        for (int i = 0; i < MENU_TOOLS.length; i++) {
            int top = this.menuY + i * MENU_ITEM_HEIGHT;
            if (mouseX >= this.menuX && mouseX < this.menuX + MENU_WIDTH
                    && mouseY >= top && mouseY < top + MENU_ITEM_HEIGHT) {
                return MENU_TOOLS[i];
            }
        }
        return null;
    }

    /** 悬停刷新（工具条按钮需要）。 */
    public void updateHover(int mouseX, int mouseY) {
        this.hoveredTool = toolAt(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isMouseOver(mouseX, mouseY) || scrollY == 0.0) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.scroll - (int) Math.signum(scrollY), maxScroll()));
        return true;
    }

    /** 滚轮事件只用于列表本身；这里暴露给界面判断区域。 */
    public boolean mouseScrolledNoop(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY) && this.data.size() > visibleRows();
    }

    // ---- 渲染 ----

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.rect.width() <= 0 || this.rect.height() <= 0) {
            return;
        }
        this.scroll = Math.clamp(this.scroll, 0, maxScroll());
        int visible = visibleRows();

        for (int i = 0; i < visible; i++) {
            int index = this.scroll + i;
            if (index >= this.data.size()) {
                break;
            }
            int top = this.rect.y() + i * ROW_HEIGHT;
            boolean isSelected = index == this.selected;
            boolean isHovered = mouseX >= this.rect.x() && mouseX < this.rect.right()
                    && mouseY >= top && mouseY < top + ROW_HEIGHT;

            int background = isSelected ? ROW_SELECTED : (isHovered ? ROW_HOVER : ROW_BG);
            if (background != ROW_BG) {
                guiGraphics.fill(this.rect.x(), top, this.rect.right() - SCROLLBAR_WIDTH - 1, top + ROW_HEIGHT, background);
            }

            TransformBlockData entry = this.data.get(index);
            boolean empty = isEmpty(entry);
            if (!empty) {
                renderIcon(guiGraphics, entry.blockState, this.rect.x() + 6, top + 2,
                        this.rect.x(), top, this.rect.right() - SCROLLBAR_WIDTH - 1, top + ROW_HEIGHT);
            } else {
                // 空槽位：画一个虚线方框占位
                guiGraphics.fill(this.rect.x() + 5, top + 4, this.rect.x() + 22, top + 5, 0x40FFFFFF);
                guiGraphics.fill(this.rect.x() + 5, top + 16, this.rect.x() + 22, top + 17, 0x40FFFFFF);
                guiGraphics.fill(this.rect.x() + 5, top + 4, this.rect.x() + 6, top + 17, 0x40FFFFFF);
                guiGraphics.fill(this.rect.x() + 21, top + 4, this.rect.x() + 22, top + 17, 0x40FFFFFF);
            }

            String slot = String.valueOf(index);
            guiGraphics.drawString(this.font, slot, this.rect.x() + 4, top + 6,
                    isSelected ? TEXT_SELECTED : TEXT_MUTED, false);

            int nameX = this.rect.x() + 20 + this.font.width(slot) + 8;
            int nameWidth = Math.max(8, this.rect.right() - SCROLLBAR_WIDTH - 3 - nameX);
            String name = empty
                    ? "（空）"
                    : this.font.plainSubstrByWidth(displayName(entry.blockState), nameWidth);
            guiGraphics.drawString(this.font, name, nameX, top + 6,
                    isSelected ? TEXT_SELECTED : (entry.isShown && !empty ? TEXT : TEXT_MUTED), false);
        }

        renderScrollbar(guiGraphics);
        renderToolbar(guiGraphics);
        renderMenu(guiGraphics);
    }

    /** 判定空槽位：AIR 且未显示。 */
    public static boolean isEmpty(TransformBlockData data) {
        return data.blockState.isAir();
    }

    private void renderToolbar(GuiGraphics guiGraphics) {
        int top = rowsBottom();
        guiGraphics.fill(this.rect.x(), top, this.rect.right(), this.rect.bottom(), 0xFF1E1E1E);
        guiGraphics.fill(this.rect.x(), top, this.rect.right(), top + 1, TOOL_BORDER);

        Tool[] tools = Tool.values();
        for (int i = 0; i < tools.length; i++) {
            int x = toolX(i);
            int y = toolY();
            boolean hovered = this.hoveredTool == tools[i];
            guiGraphics.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE, hovered ? TOOL_BG_HOVER : TOOL_BG);
            guiGraphics.fill(x, y, x + TOOL_SIZE, y + 1, TOOL_BORDER);
            guiGraphics.fill(x, y + TOOL_SIZE - 1, x + TOOL_SIZE, y + TOOL_SIZE, TOOL_BORDER);
            String symbol = tools[i].symbol;
            guiGraphics.drawString(this.font, symbol,
                    x + (TOOL_SIZE - this.font.width(symbol)) / 2, y + (TOOL_SIZE - 8) / 2,
                    hovered ? TOOL_TEXT_ACTIVE : TOOL_TEXT, false);
        }
    }

    private void renderMenu(GuiGraphics guiGraphics) {
        if (this.menuRow < 0) {
            return;
        }
        int height = MENU_TOOLS.length * MENU_ITEM_HEIGHT;
        guiGraphics.fill(this.menuX - 1, this.menuY - 1, this.menuX + MENU_WIDTH + 1, this.menuY + height + 1, TOOL_BORDER);
        guiGraphics.fill(this.menuX, this.menuY, this.menuX + MENU_WIDTH, this.menuY + height, 0xFF262626);
        for (int i = 0; i < MENU_TOOLS.length; i++) {
            int top = this.menuY + i * MENU_ITEM_HEIGHT;
            String label = Component.translatable(MENU_TOOLS[i].tooltipKey).getString();
            guiGraphics.drawString(this.font, label, this.menuX + 5, top + 4, TOOL_TEXT, false);
        }
    }

    /** 工具条按钮的 tooltip（由界面在悬停时显示）。 */
    public String hoveredToolTooltip() {
        return this.hoveredTool == null ? null
                : Component.translatable(this.hoveredTool.tooltipKey).getString();
    }

    /**
     * 画一行的方块图标。
     * <p>
     * 图标是按元素数据缩放绘制的：多元素模型常常远超一个方块的范围，会糊到相邻行与面板外，
     * 因此这里用 GL 裁剪把每个图标限制在自己这一行内（裁剪范围含行宽，横向也不会溢出）。
     * 裁剪前先 flush，避免把已排队的 2D 内容一起裁掉。
     */
    private void renderIcon(GuiGraphics guiGraphics, BlockState state, int x, int y,
                            int clipLeft, int clipTop, int clipRight, int clipBottom) {
        BakedModel model = this.minecraft.getBlockRenderer().getBlockModel(state);
        PoseStack pose = guiGraphics.pose();

        guiGraphics.flush();
        guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            pose.pushPose();
            pose.translate(x + 8.0F, y + 13.0F, 100.0F);
            pose.scale(11.0F, -11.0F, 11.0F);
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            this.minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                    guiGraphics.bufferSource().getBuffer(RenderType.TRANSLUCENT), state, model,
                    1.0F, 1.0F, 1.0F, 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int trackHeight = rowsBottom() - this.rect.y();
        int thumbHeight = Math.max(14, trackHeight * visibleRows() / Math.max(1, this.data.size()));
        int thumbY = this.rect.y() + (trackHeight - thumbHeight) * this.scroll / max;
        int x = this.rect.right() - SCROLLBAR_WIDTH;
        guiGraphics.fill(x, this.rect.y(), x + SCROLLBAR_WIDTH, rowsBottom(), 0xFF1A1A1A);
        guiGraphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF5A5A5A);
    }

    private static String displayName(BlockState state) {
        Item item = state.getBlock().asItem();
        Component name = item == Items.AIR ? state.getBlock().getName() : item.getName(item.getDefaultInstance());
        return name.getString();
    }

    /** 供界面判断点击是否落在列表上。 */
    public static boolean isLeftButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }
}
