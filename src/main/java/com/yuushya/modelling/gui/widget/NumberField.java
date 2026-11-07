package com.yuushya.modelling.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;

/**
 * 紧凑数字输入框（Blockbench / Blender 风格）。
 * <p>
 * 交互：
 * <ul>
 *   <li>按下并左右拖动 → 直接改值（数值随水平位移线性变化，按住 Shift 为精细模式）</li>
 *   <li>单击（未拖动）→ 进入文本输入，回车确认、Esc 取消</li>
 *   <li>滚轮 → 按 step 递增</li>
 * </ul>
 * 相比原版"滑块 + 加减按钮 + 输入框"三件套，一行可以放下三个轴，且不需要大块横向空间。
 */
public class NumberField extends AbstractWidget {

    private static final int BG = 0xFF212121;
    private static final int BG_HOVER = 0xFF2E2E2E;
    private static final int BG_EDITING = 0xFF12324A;
    private static final int BORDER = 0xFF0B0B0B;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int TEXT_EDITING = 0xFFFFFFFF;
    private static final int DRAG_THRESHOLD = 2;

    private final Font font;
    private final double min;
    private final double max;
    /** 每像素位移 / 每格滚轮对应的数值增量。 */
    private final double step;
    private final int decimals;
    /** 旋转角这类循环量：越界时绕回而不是夹住。 */
    private final boolean wrap;
    private final DoubleConsumer onChange;

    private double value;
    private boolean editing;
    private String buffer = "";

    private boolean dragging;
    private boolean scrubbed;
    private double dragStartX;
    private double dragStartValue;
    private int holdTicks;

    public NumberField(Font font, int x, int y, int width, int height,
                       double min, double max, double step, int decimals, boolean wrap,
                       double initial, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = decimals;
        this.wrap = wrap;
        this.onChange = onChange;
        this.value = normalize(initial);
    }

    public double getValue() {
        return this.value;
    }

    /** 外部同步（切换元素等），不触发回调。 */
    public void setValue(double value) {
        this.value = normalize(value);
        if (this.editing) {
            this.buffer = format(this.value);
        }
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void commitEdit() {
        if (!this.editing) {
            return;
        }
        this.editing = false;
        try {
            setAndNotify(Double.parseDouble(this.buffer.trim()));
        } catch (NumberFormatException ignored) {
            this.buffer = format(this.value);
        }
    }

    private void cancelEdit() {
        this.editing = false;
        this.buffer = format(this.value);
    }

    private double normalize(double raw) {
        double result = Double.isNaN(raw) ? 0.0 : raw;
        if (this.wrap) {
            double span = this.max - this.min;
            if (span > 0.0) {
                result = (result - this.min) % span;
                if (result < 0.0) {
                    result += span;
                }
                return this.min + result;
            }
        }
        return Mth.clamp(result, this.min, this.max);
    }

    private void setAndNotify(double raw) {
        double normalized = normalize(raw);
        this.value = normalized;
        this.buffer = format(normalized);
        this.onChange.accept(normalized);
    }

    private String format(double number) {
        return String.format("%." + this.decimals + "f", number);
    }

    // ---- 交互 ----

    @Override
    public void onClick(double mouseX, double mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.dragging = true;
        this.scrubbed = false;
        this.dragStartX = mouseX;
        this.dragStartValue = this.value;
        this.editing = false;
        this.buffer = format(this.value);
        this.setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging) {
            return false;
        }
        double dx = mouseX - this.dragStartX;
        if (!this.scrubbed && Math.abs(dx) >= DRAG_THRESHOLD) {
            this.scrubbed = true;
        }
        if (this.scrubbed) {
            double scale = hasShiftDown() ? 0.1 : 1.0;
            setAndNotify(this.dragStartValue + dx * this.step * scale);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.dragging) {
            return false;
        }
        this.dragging = false;
        if (!this.scrubbed) {
            this.editing = true;
            this.buffer = format(this.value);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible || scrollY == 0.0) {
            return false;
        }
        double scale = hasShiftDown() ? 0.1 : 1.0;
        setAndNotify(this.value + Math.signum(scrollY) * this.step * scale);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.editing) {
            return false;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> {
                commitEdit();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                cancelEdit();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!this.buffer.isEmpty()) {
                    this.buffer = this.buffer.substring(0, this.buffer.length() - 1);
                }
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.editing) {
            return false;
        }
        if ((codePoint >= '0' && codePoint <= '9') || codePoint == '.' || codePoint == '-' || codePoint == '+') {
            this.buffer += codePoint;
            return true;
        }
        return true;
    }

    // ---- 渲染 ----

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int background = this.editing ? BG_EDITING : (this.isHovered ? BG_HOVER : BG);
        guiGraphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, background);
        guiGraphics.fill(getX(), getY(), getX() + this.width, getY() + 1, BORDER);
        guiGraphics.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, BORDER);

        String text = this.editing ? this.buffer : format(this.value);
        text = this.font.plainSubstrByWidth(text, this.width - 6);
        guiGraphics.drawString(this.font, text, getX() + 3, getY() + (this.height - 8) / 2,
                this.editing ? TEXT_EDITING : TEXT, false);

        if (this.editing && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caret = getX() + 3 + Math.min(this.font.width(text), this.width - 7);
            guiGraphics.fill(caret, getY() + 4, caret + 1, getY() + this.height - 4, 0xFFFFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(format(this.value)));
    }

    private static boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}
