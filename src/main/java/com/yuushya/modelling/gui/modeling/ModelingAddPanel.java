package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * 「新增元素」的弹出面板，两级：
 * <ol>
 *   <li>两个选项 —— 使用副手物品 / 从背包选择</li>
 *   <li>背包选择 —— 4×9 格子（3 行主背包 + 1 行快捷栏），点击即选中</li>
 * </ol>
 * 只负责绘制与命中判定，选中后把 {@link ItemStack} 交给回调，由界面转成方块状态。
 */
public final class ModelingAddPanel {

    public enum State {
        CLOSED, MENU, INVENTORY
    }

    private static final int OPTION_HEIGHT = 16;
    private static final int MENU_WIDTH = 98;
    private static final int SLOT = 18;
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int PAD = 3;
    private static final int HEADER = 12;

    private static final int BG = 0xFF262626;
    private static final int BG_HOVER = 0xFF3C3C3C;
    private static final int BORDER = 0xFF0B0B0B;
    private static final int SLOT_BG = 0xFF1A1A1A;
    private static final int SLOT_BG_HOVER = 0xFF3A5A7A;
    private static final int TEXT = 0xFFDDDDDD;
    private static final int TITLE = 0xFF9A9A9A;

    private final Consumer<ItemStack> onPick;

    private State state = State.CLOSED;
    private int anchorX;
    private int anchorY;
    private int hoveredOption = -1;
    private int hoveredSlot = -1;

    public ModelingAddPanel(Consumer<ItemStack> onPick) {
        this.onPick = onPick;
    }

    public boolean isOpen() {
        return this.state != State.CLOSED;
    }

    /** 在给定屏幕位置弹出选项菜单。 */
    public void openAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        this.state = State.MENU;
        this.hoveredOption = -1;
        this.hoveredSlot = -1;
        this.anchorX = (int) Math.min(mouseX, screenWidth - MENU_WIDTH - 4);
        this.anchorY = (int) Math.min(mouseY, screenHeight - OPTION_HEIGHT * 2 - 4);
        this.anchorX = Math.max(2, this.anchorX);
        this.anchorY = Math.max(2, this.anchorY);
    }

    public void close() {
        this.state = State.CLOSED;
        this.hoveredOption = -1;
        this.hoveredSlot = -1;
    }

    // ---- 几何 ----

    private int gridWidth() {
        return COLS * SLOT + PAD * 2;
    }

    private int gridHeight() {
        return ROWS * SLOT + PAD * 2 + HEADER;
    }

    /** 背包面板左上角：以锚点为中心，并夹在屏幕内。 */
    private int gridX(int screenWidth) {
        return Math.max(2, Math.min(this.anchorX, screenWidth - gridWidth() - 2));
    }

    private int gridY(int screenHeight) {
        return Math.max(2, Math.min(this.anchorY, screenHeight - gridHeight() - 2));
    }

    /** 背包格子对应的物品栏下标：前 3 行是主背包（9..35），末行是快捷栏（0..8）。 */
    private static int inventoryIndex(int row, int col) {
        return row < 3 ? 9 + row * COLS + col : col;
    }

    // ---- 交互 ----

    /** 悬停刷新，返回是否需要重绘。 */
    public void updateHover(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (this.state == State.MENU) {
            this.hoveredOption = optionAt(mouseX, mouseY);
        } else if (this.state == State.INVENTORY) {
            this.hoveredSlot = slotAt(mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    private int optionAt(double mouseX, double mouseY) {
        if (mouseX < this.anchorX || mouseX >= this.anchorX + MENU_WIDTH) {
            return -1;
        }
        int index = (int) ((mouseY - this.anchorY) / OPTION_HEIGHT);
        if (mouseY < this.anchorY || index < 0 || index > 1) {
            return -1;
        }
        return index;
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
        if (this.state == State.MENU) {
            int option = optionAt(mouseX, mouseY);
            if (option < 0) {
                close();
                return true;
            }
            if (option == 0) {
                close();
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    ItemStack offhand = minecraft.player.getOffhandItem();
                    if (!offhand.isEmpty()) {
                        this.onPick.accept(offhand);
                    }
                }
            } else {
                // 切到背包选择：把锚点挪到网格左上角
                this.anchorX = (int) Math.max(2, Math.min(this.anchorX, screenWidth - gridWidth() - 2));
                this.anchorY = (int) Math.max(2, Math.min(this.anchorY, screenHeight - gridHeight() - 2));
                this.state = State.INVENTORY;
            }
            return true;
        }
        if (this.state == State.INVENTORY) {
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
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    // ---- 渲染 ----

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY,
                       int screenWidth, int screenHeight) {
        updateHover(mouseX, mouseY, screenWidth, screenHeight);
        if (this.state == State.MENU) {
            renderMenu(guiGraphics, font);
        } else if (this.state == State.INVENTORY) {
            renderInventory(guiGraphics, font, mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    private void renderMenu(GuiGraphics guiGraphics, Font font) {
        int height = OPTION_HEIGHT * 2;
        guiGraphics.fill(this.anchorX - 1, this.anchorY - 1,
                this.anchorX + MENU_WIDTH + 1, this.anchorY + height + 1, BORDER);
        guiGraphics.fill(this.anchorX, this.anchorY, this.anchorX + MENU_WIDTH, this.anchorY + height, BG);

        String[] labels = {
                Component.translatable("gui.yuushya.modeling.add.offhand").getString(),
                Component.translatable("gui.yuushya.modeling.add.inventory").getString()
        };
        for (int i = 0; i < labels.length; i++) {
            int top = this.anchorY + i * OPTION_HEIGHT;
            if (this.hoveredOption == i) {
                guiGraphics.fill(this.anchorX + 1, top, this.anchorX + MENU_WIDTH - 1, top + OPTION_HEIGHT, BG_HOVER);
            }
            guiGraphics.drawString(font, labels[i], this.anchorX + 6, top + 4, TEXT, false);
        }
    }

    private void renderInventory(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY,
                                 int screenWidth, int screenHeight) {
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

        // 悬停物品名
        if (this.hoveredSlot >= 0 && inventory != null) {
            ItemStack stack = inventory.getItem(this.hoveredSlot);
            if (!stack.isEmpty()) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }
}
