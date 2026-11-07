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
        return Math.max(1, this.rect.height() / ROW_HEIGHT);
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

    /** @return 是否消费了本次点击 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int row = this.scroll + (int) ((mouseY - this.rect.y()) / ROW_HEIGHT);
        if (row >= 0 && row < this.data.size() && row != this.selected) {
            this.selected = row;
            this.onSelect.accept(row);
        }
        return true;
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
            renderIcon(guiGraphics, entry.blockState, this.rect.x() + 6, top + 2,
                    this.rect.x(), top, this.rect.right() - SCROLLBAR_WIDTH - 1, top + ROW_HEIGHT);

            String slot = String.valueOf(index);
            guiGraphics.drawString(this.font, slot, this.rect.x() + 4, top + 6,
                    isSelected ? TEXT_SELECTED : TEXT_MUTED, false);

            int nameX = this.rect.x() + 20 + this.font.width(slot) + 8;
            int nameWidth = Math.max(8, this.rect.right() - SCROLLBAR_WIDTH - 3 - nameX);
            String name = this.font.plainSubstrByWidth(displayName(entry.blockState), nameWidth);
            guiGraphics.drawString(this.font, name, nameX, top + 6,
                    isSelected ? TEXT_SELECTED : (entry.isShown ? TEXT : TEXT_MUTED), false);
        }

        renderScrollbar(guiGraphics);
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
        int trackHeight = this.rect.height();
        int thumbHeight = Math.max(14, trackHeight * visibleRows() / this.data.size());
        int thumbY = this.rect.y() + (trackHeight - thumbHeight) * this.scroll / max;
        int x = this.rect.right() - SCROLLBAR_WIDTH;
        guiGraphics.fill(x, this.rect.y(), x + SCROLLBAR_WIDTH, this.rect.bottom(), 0xFF1A1A1A);
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
