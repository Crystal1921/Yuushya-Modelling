package com.yuushya.modelling.gui;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.gui.widget.AbstractColorScreen;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.gui.widget.TransformComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 变换编辑界面的通用基类。
 * <p>
 * ShowBlockScreen、ItemBlockScreen、TextBlockScreen 共用同一套布局常量、滑块面板管理、
 * 待发送数据暂存与槽位切换流程，这些内容统一收敛到这里；各界面只负责对接自己的
 * 变换枚举、网络包以及特有的面板控件。
 *
 * @param <T> 该界面使用的变换类型枚举
 * @param <E> 该界面编辑的方块实体类型
 */
public abstract class AbstractTransformScreen<T, E extends AbstractTransformBlockEntity> extends AbstractColorScreen {
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

    protected final E blockEntity;
    /** 尚未下发到服务端的变换改动，切换槽位或关闭界面时统一冲刷。 */
    protected final Map<T, Double> storage = new HashMap<>();
    /** 左侧滑块面板，按创建顺序保存。 */
    protected final Map<T, TransformComponent<T>> panel = new LinkedHashMap<>();
    public EditBox colorEditBox;
    public ColorWidget colorWidget;
    protected int slot;

    protected AbstractTransformScreen(Component title, E blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
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

    /** 读取指定槽位上该变换分量的当前值。 */
    protected abstract double extract(T type, int slot);

    /** 修改本地数据（不涉及网络）。 */
    protected abstract void modify(T type, int slot, double number);

    /** 将单个变换分量发送到服务端。 */
    protected abstract void sendTransform(BlockPos pos, int slot, T type, double number);

    /** 通知服务端本批次改动结束。 */
    protected abstract void sendSuccess(BlockPos pos);

    /** 该界面是否处于“编辑输入框”模式。 */
    protected abstract boolean isEditMode();

    /** 绘制左侧的槽位列表。 */
    protected abstract void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /** 绘制列表以外、滑块编辑框之前的补充信息（列表名称、属性等）。 */
    protected void renderHeader(GuiGraphics guiGraphics) {
    }

    /** 绘制滑块编辑框之后、{@code super.render} 之前的补充信息。 */
    protected void renderFooter(GuiGraphics guiGraphics) {
    }

    /** 获取（必要时创建）某个变换分量对应的滑块行。 */
    protected TransformComponent<T> choose(T type) {
        return choose(type, TransformComponent.Style.SLIDER_ROW);
    }

    protected TransformComponent<T> choose(T type, TransformComponent.Style style) {
        return panel.computeIfAbsent(type, key -> new TransformComponent<>(key, style));
    }

    /** 应用变化：暂存到 storage、修改本地数据并触发方块更新。 */
    public void updateTransformDataClient(T type, Double number) {
        this.storage.put(type, number);
        modify(type, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    /** 把 storage 中暂存的改动全部下发到服务端。 */
    protected void flushPending() {
        for (Map.Entry<T, Double> entry : this.storage.entrySet()) {
            sendTransform(blockEntity.getBlockPos(), this.slot, entry.getKey(), entry.getValue());
        }
        this.storage.clear();
    }

    /** 统一注册面板中的滑块及其附属按钮。 */
    protected final void registerPanelWidgets() {
        for (TransformComponent<T> component : this.panel.values()) {
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
        }
    }

    @Override
    public EditBox getColorEditBox() {
        return this.colorEditBox;
    }

    /** 切换槽位：冲刷暂存数据、切换槽位并刷新滑块初值。 */
    public void setSlot(int slot) {
        flushPending();
        flushSlotExtra();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (TransformComponent<T> component : this.panel.values()) {
            component.setSliderInitial(extract(component.type, slot));
        }
        onSlotChanged();
    }

    /** 切换槽位时额外需要下发到服务端的数据（物品、文本行等），默认无。 */
    protected void flushSlotExtra() {
    }

    /** 切换槽位后刷新与具体界面相关的控件状态。 */
    protected abstract void onSlotChanged();

    @Override
    public void removed() {
        flushPending();
        flushOnRemoved();
        this.storage.clear();
        sendSuccess(blockEntity.getBlockPos());
    }

    /** 关闭界面时额外需要下发到服务端的数据，默认无。 */
    protected void flushOnRemoved() {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderList(guiGraphics, mouseX, mouseY, partialTick);
        renderHeader(guiGraphics);
        if (isEditMode()) {
            for (TransformComponent<T> component : this.panel.values()) {
                guiGraphics.drawString(this.font, component.editBox.getMessage(), component.editBox.getX() + component.editBox.getWidth() / 2, component.editBox.getY() + component.editBox.getHeight() / 3, 0x707070);
                component.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        renderFooter(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
