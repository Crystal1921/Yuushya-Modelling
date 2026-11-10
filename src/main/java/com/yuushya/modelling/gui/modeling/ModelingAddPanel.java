package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * 「从背包新增元素」面板：4×9 格子（3 行主背包 + 1 行快捷栏），点击即选中。
 * <p>
 * 只负责绘制与命中判定，选中后把 {@link ItemStack} 交给回调，由界面转成方块状态。
 * 副手新增走独立按钮，不经过这里。
 */
public final class ModelingAddPanel {

    private static final int SLOT = 18;
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int PAD = 3;
    private static final int HEADER = 12;

    private static final int BG = 0xFF262626;
    private static final int BORDER = 0xFF0B0B0B;
    private static final int SLOT_BG = 0xFF1A1A1A;
    private static final int SLOT_BG_HOVER = 0xFF3A5A7A;
    private static final int TITLE = 0xFF9A9A9A;

    private final Consumer<ItemStack> onPick;

    private boolean open;
    private int anchorX;
    private int anchorY;
    private int hoveredSlot = -1;

    public ModelingAddPanel(Consumer<ItemStack> onPick) {
        this.onPick = onPick;
    }

    public boolean isOpen() {
        return this.open;
    }

    /** 在给定屏幕位置打开背包面板（绘制时自动夹在屏幕内）。 */
    public void openAt(double mouseX, double mouseY) {
        this.open = true;
        this.hoveredSlot = -1;
        this.anchorX = (int) mouseX;
        this.anchorY = (int) mouseY;
    }

    public void close() {
        this.open = false;
        this.hoveredSlot = -1;
    }

    // ---- 几何 ----

    private int gridWidth() {
        return COLS * SLOT + PAD * 2;
    }

    private int gridHeight() {
        return ROWS * SLOT + PAD * 2 + HEADER;
    }

    private int gridX(int screenWidth) {
        return Math.max(2, Math.min(this.anchorX - gridWidth() / 2, screenWidth - gridWidth() - 2));
    }

    private int gridY(int screenHeight) {
        return Math.max(2, Math.min(this.anchorY - gridHeight() / 2, screenHeight - gridHeight() - 2));
    }

    /** 背包格子对应的物品栏下标：前 3 行是主背包（9..35），末行是快捷栏（0..8）。 */
    private static int inventoryIndex(int row, int col) {
        return row < 3 ? 9 + row * COLS + col : col;
    }

    // ---- 交互 ----

    public void updateHover(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (this.open) {
            this.hoveredSlot = slotAt(mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    private int slotAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int ox = gridX(screenWidth);
        int oy = gridY(screenHeight) + HEADER;
        int col = (int) ((mouseX - ox - PAD) / SLOT);
        int row = (int) ((mouseY - oy - PAD) / SLOT);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return -1;
        }
        return inventoryIndex(row, col);
    }

    /**
     * 处理点击。
     *
     * @return 是否消费了本次点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!this.open) {
            return false;
        }
        int index = slotAt(mouseX, mouseY, screenWidth, screenHeight);
        if (index < 0) {
            close();
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (!stack.isEmpty()) {
                close();
                this.onPick.accept(stack);
            }
        }
        return true;
    }

    // ---- 渲染 ----

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY,
                       int screenWidth, int screenHeight) {
        if (!this.open) {
            return;
        }
        updateHover(mouseX, mouseY, screenWidth, screenHeight);

        int ox = gridX(screenWidth);
        int oy = gridY(screenHeight);
        guiGraphics.fill(ox - 1, oy - 1, ox + gridWidth() + 1, oy + gridHeight() + 1, BORDER);
        guiGraphics.fill(ox, oy, ox + gridWidth(), oy + gridHeight(), BG);

        Component title = Component.translatable("gui.yuushya.modeling.add.inventory");
        guiGraphics.drawString(font, title, ox + PAD, oy + 3, TITLE, false);

        Minecraft minecraft = Minecraft.getInstance();
        Inventory inventory = minecraft.player == null ? null : minecraft.player.getInventory();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = inventoryIndex(row, col);
                int sx = ox + PAD + col * SLOT;
                int sy = oy + HEADER + PAD + row * SLOT;

                boolean hovered = this.hoveredSlot == index;
                guiGraphics.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, hovered ? SLOT_BG_HOVER : SLOT_BG);

                if (inventory != null) {
                    ItemStack stack = inventory.getItem(index);
                    if (!stack.isEmpty()) {
                        guiGraphics.renderItem(stack, sx + 1, sy + 1);
                        guiGraphics.renderItemDecorations(font, stack, sx + 1, sy + 1);
                    }
                }
            }
        }

        if (this.hoveredSlot >= 0 && inventory != null) {
            ItemStack stack = inventory.getItem(this.hoveredSlot);
            if (!stack.isEmpty()) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }
}
