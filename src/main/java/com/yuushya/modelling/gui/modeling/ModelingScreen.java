package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.gui.modeling.target.ItemBlockTarget;
import com.yuushya.modelling.gui.modeling.target.ModelingChannel;
import com.yuushya.modelling.gui.modeling.target.ModelingTarget;
import com.yuushya.modelling.gui.modeling.target.ShowBlockTarget;
import com.yuushya.modelling.gui.widget.AbstractColorScreen;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 建模工作台（Blockbench 式）。
 * <p>
 * 布局：顶栏 / 左侧元素列表 / 右上元素属性 / 右下变换属性 / 中间实心 3D 视口。
 * 界面本身不认识具体方块实体，所有数据读写都通过 {@link ModelingTarget}；
 * ShowBlock 与 ItemBlock 各有一个实现，切换只需要换 target。
 */
public class ModelingScreen extends AbstractColorScreen {

    private static final int TITLE_HEIGHT = ModelingLayout.PANEL_TITLE_HEIGHT;
        private static final int ROW = 20;
    /** 右上「元素属性」的行高，比变换面板紧凑。 */
    private static final int ELEMENT_ROW = ModelingLayout.ELEMENT_ROW_HEIGHT;
    /** 放得下完整色板（色相条 + 色板 + HSV 文本）所需的最小高度，再矮就只留十六进制输入。 */
    private static final int MIN_PALETTE_HEIGHT = 121;

    private final ModelingTarget target;
    private final ModelingCamera camera = new ModelingCamera();
    private final ModelingViewport viewport = new ModelingViewport(this.camera);

    private ModelingLayout layout;
    private ModelingElementList elementList;
    private ModelingProperties properties;
    private ModelingGizmo gizmo;
    private final ModelingAxisIndicator axisIndicator = new ModelingAxisIndicator(this.camera);
    private final ModelingAddPanel addPanel = new ModelingAddPanel(this::insertElement);
    private final ModelingToolbar toolbar = new ModelingToolbar(this::onToolbarAction);
    private final ModelingOrderButtons orderButtons = new ModelingOrderButtons(this::onMove);
    private final List<Button> elementPropertyWidgets = new ArrayList<>();
    private int slot;
    /** 当前显示在右上属性区的第几组方块状态属性。 */
    private int propertyIndex;
    private Button shapeButton;
    /** 类型特有的开关行按钮（例如 ItemBlock 的物品 / 方块），没有该行时为 {@code null}。 */
    private Button toggleButton;

    private final List<AbstractWidget> transformWidgets = new ArrayList<>();
    /** 「元素属性」块占的区域；颜色模式下为 {@code null}（整块让给颜色编辑器）。 */
    private ModelingLayout.Rect elementPropertiesRect;
    /** 变换数字框 / 颜色编辑器占的区域。 */
    private ModelingLayout.Rect transformRows;
    private ColorWidget colorWidget;
    private EditBox colorEditBox;
    /** 右侧栏当前显示的是颜色编辑器而不是普通变换。 */
    private boolean colorMode;

    /** 视口内正在进行的手势：无 / 环绕视角 / 平移视角。 */
    private static final int CAMERA_NONE = 0;
    private static final int CAMERA_ORBIT = 1;
    private static final int CAMERA_PAN = 2;
    private int cameraDrag = CAMERA_NONE;

    /** 最近一次鼠标位置，供弹出面板定位。 */
    private double lastMouseX;
    private double lastMouseY;

    private ModelingScreen(ModelingTarget target) {
        super(Component.translatable("gui.yuushya.modeling.title"));
        this.target = target;
        this.slot = Math.max(0, target.slot());
    }

    /** ShowBlock 的建模界面。 */
    public static ModelingScreen forShowBlock(com.yuushya.modelling.blockentity.showblock.ShowBlockEntity entity) {
        return new ModelingScreen(new ShowBlockTarget(entity));
    }

    /** ItemBlock 的建模界面。 */
    public static ModelingScreen forItemBlock(com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity entity) {
        return new ModelingScreen(new ItemBlockTarget(entity));
    }

    @Override
    protected void init() {
        this.layout = new ModelingLayout(this.width, this.height);
        // 窗口尺寸变化会重进 init，先把上一轮的待发送改动冲刷掉
        this.target.flush();
        this.propertyIndex = 0;
        this.toolbar.setRect(this.layout.header);
        this.elementPropertiesRect = null;
        this.transformRows = null;

        // 上下移按钮贴在"元素"面板标题行右端
        ModelingLayout.Rect elementsRect = this.layout.elements;
        this.orderButtons.setRect(new ModelingLayout.Rect(elementsRect.x(), elementsRect.y(),
                elementsRect.width(), TITLE_HEIGHT));

        this.elementList = new ModelingElementList(this.font,
                this.target.elements(), this.slot, this::setSlot);
        this.elementList.setRect(new ModelingLayout.Rect(elementsRect.x() + 1, elementsRect.y() + TITLE_HEIGHT,
                elementsRect.width() - 2, elementsRect.height() - TITLE_HEIGHT - 2));
        this.elementList.setToolListener(this::onTool);

        this.properties = new ModelingProperties(this.target, this::applyValue);

        this.gizmo = new ModelingGizmo(this.camera, this.target, () -> this.slot, this::applyValue);
        this.gizmo.setRect(this.layout.viewport);

        rebuildRightPanel();
    }

    // ---- 右侧栏：元素属性 + 变换 / 颜色 ----

    /**
     * 重建整条右侧栏：面板标题行下先是「变换 / 颜色」切换，再按模式摆内容。
     * <p>
     * 变换模式：元素属性块 + 变换数字框；颜色模式：整块剩余区域都交给颜色编辑器，
     * 这样色板才有足够高度。只有能改颜色的目标（ItemBlock）才显示切换行。
     */
    private void rebuildRightPanel() {
        for (AbstractWidget widget : this.transformWidgets) {
            this.removeWidget(widget);
        }
        this.transformWidgets.clear();
        for (Button button : this.elementPropertyWidgets) {
            this.removeWidget(button);
        }
        this.elementPropertyWidgets.clear();
        this.colorWidget = null;
        this.colorEditBox = null;
        this.shapeButton = null;
        this.toggleButton = null;
        this.elementPropertiesRect = null;

        ModelingLayout.Rect panel = this.layout.right;
        boolean supportsColor = this.target.supports(ModelingChannel.COLOR_R);
        int top = panel.y() + TITLE_HEIGHT;
        if (supportsColor) {
            addTransformWidget(Button.builder(panelModeMessage(), btn -> toggleColorMode())
                    .bounds(panel.x() + 4, top, panel.width() - 8, ROW - 4)
                    .tooltip(Tooltip.create(Component.translatable("gui.yuushya.modeling.panel.tip")))
                    .build());
            top += ROW;
        }

        if (this.colorMode && supportsColor) {
            this.properties.clear();
            this.transformRows = new ModelingLayout.Rect(panel.x(), top, panel.width(),
                    Math.max(0, panel.bottom() - top));
            initColorEditor();
            return;
        }

        int propsHeight = Math.clamp(panel.bottom() - top, 0, this.layout.elementPropertiesHeight);
        this.elementPropertiesRect = new ModelingLayout.Rect(panel.x(), top, panel.width(), propsHeight);
        initElementPropertyWidgets();

        this.transformRows = new ModelingLayout.Rect(panel.x(), top + propsHeight, panel.width(),
                Math.max(0, panel.bottom() - top - propsHeight));
        this.properties.layout(this.transformRows, this.slot, this.font);
        for (AbstractWidget widget : this.properties.widgets()) {
            addTransformWidget(widget);
        }
    }

    private void addTransformWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.transformWidgets.add(widget);
    }

    /** 切换按钮上写的就是当前模式，点一下在变换与颜色之间切换。 */
    private Component panelModeMessage() {
        return Component.translatable(this.colorMode
                ? "gui.yuushya.modeling.panel.color" : "gui.yuushya.modeling.panel.transform");
    }

    /** 在「普通变换」和「颜色编辑器」之间切换。 */
    private void toggleColorMode() {
        if (!this.target.supports(ModelingChannel.COLOR_R)) {
            return;
        }
        commitInputs();
        this.colorMode = !this.colorMode;
        rebuildRightPanel();
    }

    /** 复用旧的 HSB 颜色选择器：上方十六进制输入，下方色板。 */
    private void initColorEditor() {
        ModelingLayout.Rect area = this.transformRows;
        int top = area.y();
        int width = Math.clamp(area.width() - 8, 40, 110);
        int x = area.x() + (area.width() - width) / 2;
        int color = currentColor();

        this.colorEditBox = new EditBox(this.font, x, top, width, 16,
                Component.translatable("gui.yuushya.modeling.color"));
        this.colorEditBox.setMaxLength(7);
        this.colorEditBox.setValue(String.format("#%06X", color & 0xFFFFFF));
        this.colorEditBox.setResponder(this::onColorHexChanged);
        addTransformWidget(this.colorEditBox);

        // 色板按控件尺寸自适应，但控件本身仍需要一块最小空间才放得下
        int paletteTop = top + 22;
        int paletteHeight = Math.max(0, area.bottom() - 2 - paletteTop);
        if (paletteHeight >= MIN_PALETTE_HEIGHT) {
            this.colorWidget = new ColorWidget(x, paletteTop, width, paletteHeight, color,
                    Component.translatable("gui.yuushya.modeling.color"), this);
            addTransformWidget(this.colorWidget);
        }
    }

    /** 十六进制框只认 {@code #RRGGBB}，边输边应用；其余内容无视。 */
    private void onColorHexChanged(String text) {
        if (text.length() != 7 || text.charAt(0) != '#') {
            return;
        }
        try {
            int rgb = Integer.parseInt(text.substring(1), 16);
            if (this.colorWidget != null) {
                this.colorWidget.setColor(rgb);
            }
            applyColor(rgb);
        } catch (NumberFormatException ignored) {
        }
    }

    /** 颜色选择器回调：写进当前元素的三色通道。 */
    private void applyColor(int rgb) {
        if (!this.target.supports(ModelingChannel.COLOR_R)) {
            return;
        }
        int color = FastColor.ARGB32.color(255, FastColor.ARGB32.red(rgb),
                FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb));
        if (color == currentColor()) {
            return;
        }
        this.target.set(this.slot, ModelingChannel.COLOR_R, FastColor.ARGB32.red(color));
        this.target.set(this.slot, ModelingChannel.COLOR_G, FastColor.ARGB32.green(color));
        this.target.set(this.slot, ModelingChannel.COLOR_B, FastColor.ARGB32.blue(color));
        this.target.resync();
        this.toolbar.invalidateStats();
    }

    /** 当前元素的颜色（不透明 RGB）。 */
    private int currentColor() {
        return FastColor.ARGB32.color(255,
                (int) this.target.get(this.slot, ModelingChannel.COLOR_R),
                (int) this.target.get(this.slot, ModelingChannel.COLOR_G),
                (int) this.target.get(this.slot, ModelingChannel.COLOR_B));
    }

    @Override
    public EditBox getColorEditBox() {
        return this.colorEditBox;
    }

    @Override
    public void updateColorData(int colorValue) {
        applyColor(colorValue);
    }

    /** 右侧栏「元素属性」块里的方块状态切换按钮。 */
    private void initElementPropertyWidgets() {
        for (Button button : this.elementPropertyWidgets) {
            this.removeWidget(button);
        }
        this.elementPropertyWidgets.clear();
        if (this.elementPropertiesRect == null) {
            return;
        }

        ModelingLayout.Rect rect = this.elementPropertiesRect;
            int propertyRow = rect.y() + ELEMENT_ROW;
            int valueRow = propertyRow + ELEMENT_ROW;
            int toggleRow = valueRow + ELEMENT_ROW;
            int shapeRow = rect.y() + ELEMENT_ROW * 4;
            int left = rect.x() + 4;
            int right = rect.right() - 14;

            // 上一行切「属性」，下一行切「值」
            addWidget(Button.builder(Component.literal("<"), btn -> selectProperty(-1))
                    .bounds(left, propertyRow, 10, ELEMENT_ROW).build());
            addWidget(Button.builder(Component.literal(">"), btn -> selectProperty(1))
                    .bounds(right, propertyRow, 10, ELEMENT_ROW).build());
            addWidget(Button.builder(Component.literal("<"), btn -> cycleValue(-1))
                    .bounds(left, valueRow, 10, ELEMENT_ROW).build());
            addWidget(Button.builder(Component.literal(">"), btn -> cycleValue(1))
                    .bounds(right, valueRow, 10, ELEMENT_ROW).build());

            // 类型特有的开关（ItemBlock 的物品 / 方块）：整行一个循环按钮，和形状按钮一致，没有就整行隐藏
            List<ModelingTarget.PropertyRow> toggles = this.target.toggles(this.slot);
            this.toggleButton = null;
            if (!toggles.isEmpty()) {
                ModelingTarget.PropertyRow toggle = toggles.getFirst();
                this.toggleButton = Button.builder(toggleMessage(toggle), btn -> cycleToggle())
                        .bounds(left, toggleRow, right + 10 - left, ELEMENT_ROW)
                        .tooltip(Tooltip.create(toggle.label()))
                        .build();
                addWidget(this.toggleButton);
            }

            // 形状：独立于方块状态属性，写的是方块本身的 SHAPES 属性
            this.shapeButton = Button.builder(this.target.shapeLabel(), btn -> cycleShape())
                    .bounds(left, shapeRow, right + 10 - left, ELEMENT_ROW)
                    .tooltip(Tooltip.create(Component.translatable("gui.yuushya.modeling.shape.tip")))
                    .build();
            addWidget(this.shapeButton);
        }

        private void addWidget(Button button) {
            this.addRenderableWidget(button);
            this.elementPropertyWidgets.add(button);
        }

        /** 当前元素可用的属性行。 */
        private List<ModelingTarget.PropertyRow> allProperties() {
            return this.target.properties(this.slot);
        }

        /** 当前显示的那一组属性；无可调属性时返回 {@code null}。 */
        private ModelingTarget.PropertyRow currentProperty() {
            List<ModelingTarget.PropertyRow> all = allProperties();
            if (all.isEmpty()) {
                return null;
            }
            this.propertyIndex = Math.floorMod(this.propertyIndex, all.size());
            return all.get(this.propertyIndex);
        }

        private void selectProperty(int delta) {
            List<ModelingTarget.PropertyRow> all = allProperties();
            if (all.size() <= 1) {
                return;
            }
            this.propertyIndex = Math.floorMod(this.propertyIndex + delta, all.size());
            refreshElementProperties();
        }

        private void cycleValue(int delta) {
            ModelingTarget.PropertyRow row = currentProperty();
            if (row == null) {
                return;
            }
            if (delta < 0) {
                row.previous().run();
            } else {
                row.next().run();
            }
            this.toolbar.invalidateStats();
        }

        /** 开关行：整行一个循环按钮，点一下切到下一档。 */
        private void cycleToggle() {
            List<ModelingTarget.PropertyRow> toggles = this.target.toggles(this.slot);
            if (toggles.isEmpty()) {
                return;
            }
            toggles.getFirst().next().run();
            refreshElementProperties();
            this.toolbar.invalidateStats();
        }

    /** 循环切换形状（点一下下一档），并下发到服务端。 */
    private void cycleShape() {
        Component label = this.target.cycleShape();
        if (this.shapeButton != null) {
            this.shapeButton.setMessage(label);
        }
        this.viewportDirty();
    }

    /** 属性区内容变了（切换属性 / 改值 / 换元素），按钮与文字都要跟随。 */
    private void refreshElementProperties() {
        if (this.shapeButton != null) {
            this.shapeButton.setMessage(this.target.shapeLabel());
        }
        if (this.toggleButton != null) {
            List<ModelingTarget.PropertyRow> toggles = this.target.toggles(this.slot);
            if (!toggles.isEmpty()) {
                this.toggleButton.setMessage(toggleMessage(toggles.getFirst()));
            }
        }
    }

    /** 开关按钮上的文字：左侧标签 + 右侧当前值。 */
    private static Component toggleMessage(ModelingTarget.PropertyRow toggle) {
        return Component.empty().append(toggle.label()).append(Component.literal(": ")).append(toggle.value());
    }

    /** 数据变了：刷新视口、失效面数统计。 */
    private void viewportDirty() {
        this.target.resync();
        this.toolbar.invalidateStats();
    }

    /** 切换元素：先提交正在输入的数字，再冲刷、切槽位并刷新字段。 */
    public void setSlot(int slot) {
        if (slot == this.slot || slot < 0 || slot >= this.target.size()) {
            return;
        }
        commitInputs();
        this.slot = slot;
        this.target.setSlot(slot);
        this.propertyIndex = 0;
        this.elementList.setSelected(slot);
        rebuildRightPanel();
    }

    /** 提交正在输入的数字框并冲刷待发送的改动。 */
    private void commitInputs() {
        if (this.properties != null) {
            this.properties.commitAll();
        }
        this.target.flush();
    }

    @Override
    public void removed() {
        commitInputs();
        this.target.commit();
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
        this.viewport.render(guiGraphics, this.layout.viewport, this.target);
        this.gizmo.setRect(this.layout.viewport);
        this.gizmo.renderGrid(guiGraphics, this.width, this.height);
        this.gizmo.render(guiGraphics, this.width, this.height);
        this.axisIndicator.render(guiGraphics, this.layout.viewport, mouseX, mouseY);
        ModelingChrome.render(guiGraphics, this.font, this.layout, this.target.displayName(), null);
        this.toolbar.render(guiGraphics, this.font, mouseX, mouseY);
        this.orderButtons.setBounds(this.slot > 0, this.slot < this.target.size() - 1);
        this.orderButtons.render(guiGraphics, this.font, mouseX, mouseY);
        this.elementList.render(guiGraphics, mouseX, mouseY);
        renderTransformLabels(guiGraphics);
        renderElementProperties(guiGraphics);
        renderStatus(guiGraphics);
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        // 弹出面板最后画，压在一切之上
        this.addPanel.render(guiGraphics, this.font, mouseX, mouseY, this.width, this.height);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    /** 统一渲染各处自绘控件的悬停提示（工具栏 / 元素工具条 / 上下移按钮）。 */
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 弹出面板打开时不显示，避免叠在一起
        if (this.addPanel.isOpen()) {
            return;
        }
        String text = this.toolbar.hoveredTooltip();
        if (text != null) {
            guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
            return;
        }
        text = this.elementList.hoveredToolTooltip();
        if (text != null) {
            guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
            return;
        }
        text = this.orderButtons.hoveredTooltip();
        if (text != null) {
            guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
        }
    }

    /** 右下每行的左侧标签。 */
    private void renderTransformLabels(GuiGraphics guiGraphics) {
        if (this.transformRows == null) {
            return;
        }
        ModelingLayout.Rect rect = this.transformRows;
        for (var entry : this.properties.labels().entrySet()) {
            int y = rect.y() + TITLE_HEIGHT + entry.getKey() * ROW + 6;
            guiGraphics.drawString(this.font, entry.getValue(), rect.x() + 5, y, ModelingChrome.PANEL_TITLE, false);
        }
    }

    /** 右侧栏顶部：元素名、属性名、属性值（颜色模式下整块让给颜色编辑器）。 */
    private void renderElementProperties(GuiGraphics guiGraphics) {
        ModelingLayout.Rect rect = this.elementPropertiesRect;
        if (rect == null) {
            return;
        }
        int y = rect.y();
        int textOffset = (ELEMENT_ROW - 9) / 2;
        guiGraphics.drawString(this.font, this.target.displayName(), rect.x() + 5, y + textOffset, 0xFFFFFFFF, false);

        ModelingTarget.PropertyRow row = currentProperty();
                if (row == null) {
                    guiGraphics.drawString(this.font, Component.translatable("gui.yuushya.modeling.noProperties"),
                            rect.x() + 5, y + ELEMENT_ROW + textOffset, ModelingChrome.PANEL_TITLE, false);
                    return;
                }
                guiGraphics.drawString(this.font, row.label(), centered(rect, row.label().getString()),
                        y + ELEMENT_ROW + textOffset, 0xFFB0B0B0, false);
                guiGraphics.drawString(this.font, row.value(), centered(rect, row.value().getString()),
                        y + ELEMENT_ROW * 2 + textOffset, 0xFFFFFFFF, false);
            }

    private int centered(ModelingLayout.Rect rect, String text) {
        return rect.x() + (rect.width() - this.font.width(text)) / 2;
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        ModelingLayout.Rect footer = this.layout.footer;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.yuushya.modeling.status", this.target.size(), this.slot),
                footer.x() + 5, footer.y() + 4, ModelingChrome.TEXT, false);
        Component right = Component.translatable("gui.yuushya.modeling.cameraHint");
        guiGraphics.drawString(this.font, right, footer.right() - 5 - this.font.width(right),
                footer.y() + 4, ModelingChrome.PANEL_TITLE, false);
    }

    // ---- 元素增删改 ----

    /** 上移 / 下移当前元素（与相邻元素交换）。 */
    private void onMove(ModelingOrderButtons.Direction direction) {
        moveElement(direction == ModelingOrderButtons.Direction.UP ? -1 : 1);
    }

    /**
     * 把当前元素与相邻元素交换位置。
     * <p>
     * 服务端没有"交换"语义，所以交换后要把这两个槽位重新下发一遍。
     *
     * @param delta -1 上移，+1 下移
     */
    private void moveElement(int delta) {
        int size = this.target.size();
        int from = this.slot;
        int to = from + delta;
        if (size <= 1 || to < 0 || to >= size) {
            return;
        }
        commitInputs();

                this.target.swap(from, to);

                this.slot = to;
                this.target.setSlot(to);
                this.elementList.setSelected(to);
                this.elementList.markDirty();
                this.toolbar.invalidateStats();
                this.propertyIndex = 0;
                rebuildRightPanel();
                viewportDirty();
            }

    /** 顶栏动作分发。 */
    private void onToolbarAction(ModelingToolbar.Action action) {
        switch (action) {
            case COPY -> copyAll();
            case PASTE -> pasteAll();
            case SAVE -> saveToFile();
            case STATS -> this.toolbar.setFaceCount(this.target.faceCount());
            case HELP -> this.minecraft.setScreen(new ModelingHelpScreen(this));
        }
    }

    /** 复制：把整个模型序列化到剪贴板。 */
    private void copyAll() {
        commitInputs();
        ClientMethod.setClipboard(this.target.serialize());
        toast(Component.translatable("gui.yuushya.modeling.action.copy.done"));
    }

    /** 粘贴：完全替换当前模型的全部元素。 */
    private void pasteAll() {
        String clipboard = ClientMethod.getClipboard();
        commitInputs();
        if (!this.target.paste(clipboard)) {
            toast(Component.translatable("gui.yuushya.modeling.action.paste.fail"));
            return;
        }
        this.slot = Math.clamp(this.target.size() - 1, 0, this.slot);
        this.target.setSlot(this.slot);
        this.propertyIndex = 0;
        this.elementList.setSelected(this.slot);
        this.elementList.markDirty();
        this.toolbar.invalidateStats();
        rebuildRightPanel();
        viewportDirty();
        toast(Component.translatable("gui.yuushya.modeling.action.paste.done"));
    }

    /** 保存：弹命名对话框后写入 modellings 目录。 */
    private void saveToFile() {
        commitInputs();
        this.minecraft.setScreen(new com.yuushya.modelling.gui.showblock.EditScreen(this,
                Component.translatable("gui.showBlockScreen.workshop.save"),
                Component.translatable("gui.showBlockScreen.workshop.save.tip"),
                name -> {
                    if (name != null && !name.isBlank()) {
                        try {
                            this.target.saveToFile(name);
                            toast(Component.translatable("gui.yuushya.modeling.action.save.done"));
                        } catch (Exception e) {
                            toast(Component.literal(String.valueOf(e.getMessage())));
                        }
                    }
                    this.minecraft.setScreen(this);
                },
                name -> true));
    }

    private void toast(Component message) {
        this.minecraft.getToasts().addToast(new SystemToast(
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.translatable("gui.yuushya.modeling.title"), message));
    }

    /**
     * 工具条 / 右键菜单的动作分发。
     */
    private void onTool(ModelingElementList.Tool tool) {
        switch (tool) {
            case ADD_OFFHAND -> addFromOffhand();
            case ADD_INVENTORY -> addFromInventory();
            case DUPLICATE -> duplicateElement();
            case REMOVE -> removeElement();
            case TOGGLE_SHOWN -> toggleShown();
        }
    }

    /** 用副手物品新增（副手不能用时给提示）。 */
    private void addFromOffhand() {
        if (this.minecraft.player == null) {
            return;
        }
        ItemStack offhand = this.minecraft.player.getOffhandItem();
        if (!this.target.accepts(offhand)) {
            toast(Component.translatable("gui.yuushya.modeling.add.offhand.empty"));
            return;
        }
        insertElement(offhand);
    }

    /** 打开背包选择面板新增。 */
    private void addFromInventory() {
        this.addPanel.openAt(this.lastMouseX, this.lastMouseY);
    }

    /**
     * 把一个物品作为新元素写入模型。
     * <p>
     * 若当前选中的是空槽位（例如刚删过或本来就空着），直接<b>复用该槽位</b>而不是在后面追加，
     * 避免空行堆积；否则新增到末尾。
     */
    private void insertElement(ItemStack stack) {
        if (!this.target.accepts(stack)) {
            return;
        }
        commitInputs();

        int targetSlot;
        boolean reuse = this.slot >= 0 && this.slot < this.target.size()
                && this.target.isEmptyElement(this.slot);
        if (reuse) {
            targetSlot = this.slot;
            this.target.element(targetSlot).reset();
        } else {
            targetSlot = this.target.appendEmpty();
        }
        this.target.setContent(targetSlot, stack.copy());
        this.target.setLocal(targetSlot, ModelingChannel.SHOWN, 1.0);

        this.target.resend(targetSlot, this.target.element(targetSlot));
        this.slot = targetSlot;
        this.target.setSlot(targetSlot);
        this.propertyIndex = 0;
        this.elementList.setSelected(targetSlot);
        this.elementList.markDirty();
        rebuildRightPanel();
        viewportDirty();
    }

    /** 复制当前元素，原样重叠并选中副本。 */
    private void duplicateElement() {
        if (this.target.size() <= 0 || this.target.isEmptyElement(this.slot)) {
            return;
        }
        commitInputs();

        int newSlot = this.target.appendEmpty();
        this.target.element(newSlot).setFrom(this.target.element(this.slot));

        this.target.resend(newSlot, this.target.element(newSlot));
        this.slot = newSlot;
        this.target.setSlot(newSlot);
        this.propertyIndex = 0;
        this.elementList.setSelected(newSlot);
        this.elementList.markDirty();
        rebuildRightPanel();
        viewportDirty();
    }

    /**
     * 删除当前元素：真正从列表移除，后续元素整体前移，并重新下发受影响的槽位。
     */
    private void removeElement() {
        int size = this.target.size();
        if (size <= 0) {
            return;
        }
        commitInputs();

        int removedAt = this.slot;
        this.target.removeAt(removedAt);
        if (this.target.size() <= 0) {
            // 至少保留一个空槽位，否则界面无从显示
            this.target.appendEmpty();
        }

        for (int i = removedAt; i < this.target.size(); i++) {
            this.target.resend(i, this.target.element(i));
        }
        this.target.clearTail(this.target.size(), size);

        int newSlot = Math.min(removedAt, this.target.size() - 1);
        this.slot = newSlot;
        this.target.setSlot(newSlot);
        this.propertyIndex = 0;
        this.elementList.setSelected(newSlot);
        this.elementList.markDirty();
        rebuildRightPanel();
        viewportDirty();
    }

    /** 切换当前元素的显示状态。 */
    private void toggleShown() {
        if (this.target.size() <= 0) {
            return;
        }
        double shown = this.target.get(this.slot, ModelingChannel.SHOWN) != 0.0 ? 0.0 : 1.0;
        applyValue(ModelingChannel.SHOWN, shown);
    }

    /**
     * 写值：改本地数据 + 立即重渲染。真正的发包交给 {@link ModelingTarget#flush()}。
     */
    private void applyValue(ModelingChannel channel, double value) {
        if (!this.target.supports(channel)) {
            return;
        }
            if (channel.isScale() && (this.properties == null || this.properties.isUniformScale())) {
                // 勾了「等比」：拖任意一轴都同步三轴（0 会造成退化矩阵，夹到 1）
                double effective = Math.abs(value) <= 0.001 ? 1.0 : value;
                for (ModelingChannel axis : new ModelingChannel[]{
                        ModelingChannel.SCALE_X, ModelingChannel.SCALE_Y, ModelingChannel.SCALE_Z}) {
                    this.target.set(this.slot, axis, effective);
                    if (this.properties != null) {
                        this.properties.sync(axis, effective);
                    }
                }
            } else {
                this.target.set(this.slot, channel, value);
                if (this.properties != null) {
                    this.properties.sync(channel, value);
                }
            }
            this.target.resync();
            // 几何变了，之前统计的面数不再成立
            this.toolbar.invalidateStats();
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
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        // 弹出面板优先：打开时吞掉所有点击
        if (this.addPanel.isOpen()) {
            this.addPanel.mouseClicked(mouseX, mouseY, this.width, this.height);
            return true;
        }
        // 任何点击都先提交正在输入的数字，避免切换焦点后丢掉编辑内容
        if (this.properties != null) {
            this.properties.commitAll();
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.toolbar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.orderButtons.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.elementList.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && this.elementList.mouseRightClicked(mouseX, mouseY)) {
            return true;
        }
        if (this.elementList.isMenuOpen()) {
            this.elementList.closeMenu();
        }
        if (isOverViewport(mouseX, mouseY)) {
            boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;

            if (left && this.axisIndicator.mouseClicked(this.layout.viewport, mouseX, mouseY)) {
                return true;
            }
            // 1) 命中轴线 → 左键平移、右键绕轴旋转
            if ((left || right) && this.gizmo.begin(mouseX, mouseY, right, this.width, this.height)) {
                return true;
            }
            // 2) 未命中轴线 → 左键环绕视角、右键平移视角（中键同样环绕）
            if (left || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                this.cameraDrag = CAMERA_ORBIT;
                return true;
            }
            if (right) {
                this.cameraDrag = CAMERA_PAN;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.toolbar.updateHover(mouseX, mouseY);
        this.orderButtons.updateHover(mouseX, mouseY);
        this.elementList.updateHover((int) mouseX, (int) mouseY);
        if (isOverViewport(mouseX, mouseY)) {
            this.gizmo.hover(mouseX, mouseY, this.width, this.height);
        } else {
            this.gizmo.clearHover();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.cameraDrag == CAMERA_ORBIT) {
            this.camera.orbit(dragX, dragY);
            return true;
        }
        if (this.cameraDrag == CAMERA_PAN) {
            this.camera.pan(dragX, dragY);
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
        if (this.cameraDrag != CAMERA_NONE) {
            this.cameraDrag = CAMERA_NONE;
            return true;
        }
        if (this.gizmo.isActive()) {
            this.gizmo.end();
            this.target.flush();
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.addPanel.isOpen()) {
            this.addPanel.close();
            return true;
        }
        // 十六进制颜色框在输入时，快捷键让位给文本编辑
        if (this.getFocused() instanceof EditBox) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        boolean ctrl = hasControlDown();
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            removeElement();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_D) {
            duplicateElement();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_N) {
            addFromInventory();
            return true;
        }
        if (hasShiftDown() && keyCode == GLFW.GLFW_KEY_UP) {
            moveElement(-1);
            return true;
        }
        if (hasShiftDown() && keyCode == GLFW.GLFW_KEY_DOWN) {
            moveElement(1);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copyAll();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            pasteAll();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_S) {
            saveToFile();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.elementList.isMenuOpen()) {
            this.elementList.closeMenu();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
