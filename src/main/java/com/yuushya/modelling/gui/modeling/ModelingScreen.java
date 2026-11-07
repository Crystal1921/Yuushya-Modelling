package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.network.TransformDataOncePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * 建模工作台（Blockbench 式）。
 * <p>
 * 布局：顶栏 / 左侧元素列表 / 右上元素属性（方块状态）/ 右下变换属性 / 中间实心 3D 视口。
 * 属性编辑使用紧凑的 {@link com.yuushya.modelling.gui.widget.NumberField}（拖动即改值、单击即输入），
 * 元素列表是自绘的紧凑行，均不使用原版滑块与 {@code ObjectSelectionList}。
 */
public class ModelingScreen extends Screen {

    private static final int TITLE_HEIGHT = ModelingLayout.PANEL_TITLE_HEIGHT;
    private static final int ROW = 20;

    private final ShowBlockEntity blockEntity;
    private final ModelingCamera camera = new ModelingCamera();
    private final ModelingViewport viewport = new ModelingViewport(this.camera);
    private final Map<TransformType, Double> storage = new HashMap<>();

    private ModelingLayout layout;
    private ModelingElementList elementList;
    private ModelingProperties properties;
    private ModelingGizmo gizmo;
    private final ModelingAxisIndicator axisIndicator = new ModelingAxisIndicator(this.camera);
    private int slot;
    private Property<?> property;
    private Button prevProperty;
    private Button nextProperty;
    private Button prevValue;
    private Button nextValue;
    private boolean orbiting;

    public ModelingScreen(ShowBlockEntity blockEntity) {
        super(Component.translatable("gui.yuushya.modeling.title"));
        this.blockEntity = blockEntity;
        this.slot = Math.max(0, blockEntity.getSlot());
    }

    @Override
    protected void init() {
        this.layout = new ModelingLayout(this.width, this.height);
        // 窗口尺寸变化会重进 init，先把上一轮的待发送改动冲刷掉
        flushPending();
        this.property = null;

        ModelingLayout.Rect elements = this.layout.elements;
        this.elementList = new ModelingElementList(this.minecraft, this.font,
                this.blockEntity.getTransformData(), this.slot, this::setSlot);
        this.elementList.setRect(new ModelingLayout.Rect(elements.x() + 1, elements.y() + TITLE_HEIGHT,
                elements.width() - 2, elements.height() - TITLE_HEIGHT - 2));

        this.properties = new ModelingProperties(this.blockEntity, this::applyValue);
        this.properties.layout(this.layout.transform, this.slot, this.font);
        for (AbstractWidget widget : this.properties.widgets()) {
            this.addRenderableWidget(widget);
        }

        this.gizmo = new ModelingGizmo(this.camera, this.blockEntity, () -> this.slot, this::applyValue,
                this.viewport::currentStack);
        this.gizmo.setRect(this.layout.viewport);

        initElementPropertyWidgets();
    }

    /** 右上「元素属性」里的方块状态切换按钮。 */
    private void initElementPropertyWidgets() {
        ModelingLayout.Rect rect = this.layout.elementProperties;
        int propertyRow = rect.y() + TITLE_HEIGHT + ROW;
        int valueRow = rect.y() + TITLE_HEIGHT + ROW * 2;
        int left = rect.x() + 4;
        int right = rect.right() - 14;

        this.prevProperty = Button.builder(Component.literal("<"),
                        btn -> this.property = YuushyaBlockStates.getRelative(properties(), this.property, true))
                .bounds(left, propertyRow, 10, ROW).build();
        this.nextProperty = Button.builder(Component.literal(">"),
                        btn -> this.property = YuushyaBlockStates.getRelative(properties(), this.property, false))
                .bounds(right, propertyRow, 10, ROW).build();
        this.prevValue = Button.builder(Component.literal("<"), btn -> cycleValue(true))
                .bounds(left, valueRow, 10, ROW).build();
        this.nextValue = Button.builder(Component.literal(">"), btn -> cycleValue(false))
                .bounds(right, valueRow, 10, ROW).build();

        this.addRenderableWidget(this.prevProperty);
        this.addRenderableWidget(this.nextProperty);
        this.addRenderableWidget(this.prevValue);
        this.addRenderableWidget(this.nextValue);
    }

    private void cycleValue(boolean previous) {
        Property<?> current = resolveProperty();
        if (current == null) {
            return;
        }
        applyValue(TransformType.BLOCK_STATE,
                Block.getId(YuushyaBlockStates.cycleState(getBlockState(), current, previous)));
    }

    private java.util.Collection<Property<?>> properties() {
        return getBlockState().getBlock().getStateDefinition().getProperties();
    }

    private Property<?> resolveProperty() {
        if (this.property != null) {
            return this.property;
        }
        var all = properties();
        this.property = all.isEmpty() ? null : all.iterator().next();
        return this.property;
    }

    private BlockState getBlockState() {
        return this.blockEntity.getTransformData(this.slot).blockState;
    }

    // ---- 数据同步 ----

    /** 写值：暂存 + 立即改本地数据 + 触发重渲染。 */
    private void applyValue(TransformType type, double value) {
        this.storage.put(type, value);
        type.modify(this.blockEntity, this.slot, value);
        this.blockEntity.getLevel().sendBlockUpdated(this.blockEntity.getBlockPos(),
                this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.viewport.invalidate();
        if (this.properties != null) {
            this.properties.sync(type, value);
        }
    }

    private void flushPending() {
        for (Map.Entry<TransformType, Double> entry : this.storage.entrySet()) {
            TransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, entry.getKey(), entry.getValue());
        }
        this.storage.clear();
    }

    /** 切换元素：先提交正在输入的数字，再冲刷、切槽位并刷新字段。 */
    public void setSlot(int slot) {
        if (slot == this.slot) {
            return;
        }
        if (this.properties != null) {
            this.properties.commitAll();
        }
        flushPending();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        this.property = null;
        if (this.properties != null) {
            this.properties.refresh(slot);
        }
        this.elementList.setSelected(slot);
        this.viewport.invalidate();
    }

    @Override
    public void removed() {
        if (this.properties != null) {
            this.properties.commitAll();
        }
        flushPending();
        TransformDataOncePacket.sendToServerSideSuccess(this.blockEntity.getBlockPos());
    }

    // ---- 渲染 ----

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, ModelingChrome.CHROME_BG);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.layout == null) {
            this.layout = new ModelingLayout(this.width, this.height);
        }
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.viewport.render(guiGraphics, this.layout.viewport, this.blockEntity, this.blockEntity.getTransformData());
        this.gizmo.setRect(this.layout.viewport);
        this.gizmo.renderGrid(guiGraphics, this.width, this.height);
        this.gizmo.render(guiGraphics, this.width, this.height);
        this.axisIndicator.render(guiGraphics, this.layout.viewport, mouseX, mouseY);
        ModelingChrome.render(guiGraphics, this.font, this.layout, currentElementName(), null);
        this.elementList.render(guiGraphics, mouseX, mouseY);
        renderTransformLabels(guiGraphics);
        renderElementProperties(guiGraphics);
        renderStatus(guiGraphics);
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /** 右下每行的左侧标签。 */
    private void renderTransformLabels(GuiGraphics guiGraphics) {
        ModelingLayout.Rect rect = this.layout.transform;
        for (Map.Entry<Integer, Component> entry : this.properties.labels().entrySet()) {
            int y = rect.y() + TITLE_HEIGHT + entry.getKey() * ROW + 6;
            guiGraphics.drawString(this.font, entry.getValue(), rect.x() + 5, y, ModelingChrome.PANEL_TITLE, false);
        }
    }

    /** 右上：方块名、属性名、属性值。 */
    private void renderElementProperties(GuiGraphics guiGraphics) {
        ModelingLayout.Rect rect = this.layout.elementProperties;
        int y = rect.y() + TITLE_HEIGHT;
        guiGraphics.drawString(this.font, getBlockState().getBlock().getName(), rect.x() + 5, y, 0xFFFFFFFF, false);

        Property<?> current = resolveProperty();
        if (current == null) {
            guiGraphics.drawString(this.font, Component.translatable("gui.yuushya.modeling.noProperties"),
                    rect.x() + 5, y + ROW, ModelingChrome.PANEL_TITLE, false);
            return;
        }
        String propertyName = current.getName();
        String valueName = valueText(current, getBlockState());
        guiGraphics.drawString(this.font, propertyName, centered(rect, propertyName), y + ROW + 6, 0xFFB0B0B0, false);
        guiGraphics.drawString(this.font, valueName, centered(rect, valueName), y + ROW * 2 + 6, 0xFFFFFFFF, false);
    }

    private int centered(ModelingLayout.Rect rect, String text) {
        return rect.x() + (rect.width() - this.font.width(text)) / 2;
    }

    private static <T extends Comparable<T>> String valueText(Property<T> property, BlockState state) {
        return property.getName(state.getValue(property));
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        ModelingLayout.Rect footer = this.layout.footer;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.yuushya.modeling.status", this.blockEntity.size(), this.slot),
                footer.x() + 5, footer.y() + 4, ModelingChrome.TEXT, false);
        Component right = Component.translatable("gui.yuushya.modeling.cameraHint");
        guiGraphics.drawString(this.font, right, footer.right() - 5 - this.font.width(right),
                footer.y() + 4, ModelingChrome.PANEL_TITLE, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 输入分发 ----

    private boolean isOverViewport(double mouseX, double mouseY) {
        return this.layout != null && this.layout.viewport.contains(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 任何点击都先提交正在输入的数字，避免切换焦点后丢掉编辑内容
        if (this.properties != null) {
            this.properties.commitAll();
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.elementList.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (isOverViewport(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    && this.axisIndicator.mouseClicked(this.layout.viewport, mouseX, mouseY)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                this.orbiting = true;
                return true;
            }
            boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            if ((left || right) && this.gizmo.begin(mouseX, mouseY, right, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isOverViewport(mouseX, mouseY)) {
            this.gizmo.hover(mouseX, mouseY, this.width, this.height);
        } else {
            this.gizmo.clearHover();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.orbiting) {
            this.camera.orbit(dragX, dragY);
            return true;
        }
        if (this.gizmo.isActive()) {
            this.gizmo.update(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.orbiting) {
            this.orbiting = false;
            return true;
        }
        if (this.gizmo.isActive()) {
            this.gizmo.end();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.elementList.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (isOverViewport(mouseX, mouseY) && scrollY != 0.0) {
            this.camera.zoom(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Component currentElementName() {
        if (this.blockEntity.size() <= 0) {
            return Component.translatable("gui.yuushya.modeling.empty");
        }
        return getBlockState().getBlock().getName();
    }
}
