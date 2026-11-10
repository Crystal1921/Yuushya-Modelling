package com.yuushya.modelling.gui.modeling;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.utils.ClientMethod;
import com.yuushya.modelling.utils.ShareUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
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
    private final ModelingAddPanel addPanel = new ModelingAddPanel(this::insertElement);
    private final ModelingToolbar toolbar = new ModelingToolbar(this::onToolbarAction);
    private final ModelingOrderButtons orderButtons = new ModelingOrderButtons(this::onMove);
    private int slot;
    private Property<?> property;
    private Button shapeButton;
    private final List<BlockShape> SHAPES = List.of(BlockShape.values());

    /** 视口内正在进行的手势：无 / 环绕视角 / 平移视角。 */
    private static final int CAMERA_NONE = 0;
    private static final int CAMERA_ORBIT = 1;
    private static final int CAMERA_PAN = 2;
    private int cameraDrag = CAMERA_NONE;

    /** 最近一次鼠标位置，供弹出面板定位。 */
    private double lastMouseX;
    private double lastMouseY;

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
        this.toolbar.setRect(this.layout.header);
        // 上下移按钮贴在"元素"面板标题行右端
        ModelingLayout.Rect elementsRect = this.layout.elements;
        this.orderButtons.setRect(new ModelingLayout.Rect(elementsRect.x(), elementsRect.y(),
                elementsRect.width(), TITLE_HEIGHT));

        ModelingLayout.Rect elements = this.layout.elements;
        this.elementList = new ModelingElementList(this.minecraft, this.font,
                this.blockEntity.getTransformData(), this.slot, this::setSlot);
        this.elementList.setRect(new ModelingLayout.Rect(elements.x() + 1, elements.y() + TITLE_HEIGHT,
                elements.width() - 2, elements.height() - TITLE_HEIGHT - 2));
        this.elementList.setToolListener(this::onTool);

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
        int shapeRow = rect.y() + TITLE_HEIGHT + ROW * 3;
        int left = rect.x() + 4;
        int right = rect.right() - 14;

        Button prevProperty = Button.builder(Component.literal("<"),
                        btn -> this.property = YuushyaBlockStates.getRelative(properties(), this.property, true))
                .bounds(left, propertyRow, 10, ROW).build();
        Button nextProperty = Button.builder(Component.literal(">"),
                        btn -> this.property = YuushyaBlockStates.getRelative(properties(), this.property, false))
                .bounds(right, propertyRow, 10, ROW).build();
        Button prevValue = Button.builder(Component.literal("<"), btn -> cycleValue(true))
                .bounds(left, valueRow, 10, ROW).build();
        Button nextValue = Button.builder(Component.literal(">"), btn -> cycleValue(false))
                .bounds(right, valueRow, 10, ROW).build();

        // 形状：独立于方块状态属性，写的是方块本身的 SHAPES 属性
        this.shapeButton = Button.builder(shapeSymbol(), btn -> cycleShape())
                .bounds(left, shapeRow, right + 10 - left, ROW)
                .tooltip(Tooltip.create(Component.translatable("gui.yuushya.modeling.shape.tip")))
                .build();

        this.addRenderableWidget(prevProperty);
        this.addRenderableWidget(nextProperty);
        this.addRenderableWidget(prevValue);
        this.addRenderableWidget(nextValue);
        this.addRenderableWidget(this.shapeButton);
    }

    /** 当前方块形状的显示文本。 */
    private Component shapeSymbol() {
        return this.blockEntity.getBlockState().getValue(YuushyaBlockStates.SHAPES).getSymbol();
    }

    /** 循环切换形状（点一下下一档），并下发到服务端。 */
    private void cycleShape() {
        BlockShape current = this.blockEntity.getBlockState().getValue(YuushyaBlockStates.SHAPES);
        BlockShape next = YuushyaBlockStates.getRelative(SHAPES, current, true);
        if (next == null) {
            return;
        }
        this.storage.put(TransformType.SHAPE, (double) next.ordinal());
        TransformType.SHAPE.modify(this.blockEntity, this.slot, next.ordinal());
        flushPending();
        this.blockEntity.getLevel().sendBlockUpdated(this.blockEntity.getBlockPos(),
                this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        if (this.shapeButton != null) {
            this.shapeButton.setMessage(shapeSymbol());
        }
        this.viewport.invalidate();
        this.toolbar.invalidateStats();
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
        // 几何变了，之前统计的面数不再成立
        this.toolbar.invalidateStats();
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
        this.toolbar.render(guiGraphics, this.font, mouseX, mouseY);
        this.orderButtons.setBounds(this.slot > 0, this.slot < this.blockEntity.size() - 1);
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
        int size = this.blockEntity.size();
        int from = this.slot;
        int to = from + delta;
        if (size <= 1 || to < 0 || to >= size) {
            return;
        }
        this.properties.commitAll();
        flushPending();

        List<TransformBlockData> data = this.blockEntity.getTransformData();
        TransformBlockData moving = data.get(from);
        TransformBlockData other = data.get(to);
        data.set(from, other);
        data.set(to, moving);

        pushElement(from, data.get(from));
        pushElement(to, data.get(to));

        this.slot = to;
        this.blockEntity.setSlot(to);
        this.elementList.setSelected(to);
        this.elementList.markDirty();
        this.toolbar.invalidateStats();
        this.blockEntity.getLevel().sendBlockUpdated(this.blockEntity.getBlockPos(),
                this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.viewport.invalidate();
    }

    /** 顶栏动作分发。 */
    private void onToolbarAction(ModelingToolbar.Action action) {
        switch (action) {
            case COPY -> copyAll();
            case PASTE -> pasteAll();
            case SAVE -> saveToFile();
            case STATS -> this.toolbar.setFaceCount(ShowBlockScreen.calculateFaces(this.blockEntity));
            case HELP -> this.minecraft.setScreen(new ModelingHelpScreen(this));
        }
    }

    /** 复制：把整个模型序列化到剪贴板。 */
    private void copyAll() {
        this.properties.commitAll();
        flushPending();
        ClientMethod.setClipboard(ShareUtils.transfer(this.blockEntity.getTransformData()));
        toast(Component.translatable("gui.yuushya.modeling.action.copy.done"));
    }

    /**
     * 粘贴：<b>完全替换</b>当前模型的全部元素。
     * <p>
     * {@code ShareBlockInformation.transfer(list)} 会清空并重填列表，因此这里先收缩到新元素数量、
     * 把每个槽位下发一遍，再把多余的旧槽位清掉 —— 与「删除」采用的真删除语义保持一致。
     */
    private void pasteAll() {
        String clipboard = ClientMethod.getClipboard();
        ShareUtils.ShareBlockInformation info;
        try {
            info = ShareUtils.from(clipboard);
        } catch (Exception e) {
            toast(Component.translatable("gui.yuushya.modeling.action.paste.fail"));
            return;
        }
        if (info == null || info.blocks().isEmpty()) {
            toast(Component.translatable("gui.yuushya.modeling.action.paste.fail"));
            return;
        }
        this.properties.commitAll();
        flushPending();

        int oldSize = this.blockEntity.size();
        List<TransformBlockData> data = this.blockEntity.getTransformData();
        info.transfer(data);
        if (data.isEmpty()) {
            data.add(new TransformBlockData());
        }

        // 新元素逐个下发
        for (int i = 0; i < data.size(); i++) {
            pushElement(i, data.get(i));
        }
        // 旧模型多出来的槽位清空
        for (int i = data.size(); i < oldSize; i++) {
            TransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), i, TransformType.REMOVE, 0.0);
        }

        int newSlot = Math.min(this.slot, this.blockEntity.size() - 1);
        this.slot = Math.max(0, newSlot);
        this.blockEntity.setSlot(this.slot);
        this.property = null;
        this.properties.refresh(this.slot);
        this.elementList.setSelected(this.slot);
        this.elementList.markDirty();
        this.toolbar.invalidateStats();
        this.blockEntity.getLevel().sendBlockUpdated(this.blockEntity.getBlockPos(),
                this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.viewport.invalidate();
        toast(Component.translatable("gui.yuushya.modeling.action.paste.done"));
    }

    /** 保存：弹命名对话框后写入 modellings 目录。 */
    private void saveToFile() {
        this.properties.commitAll();
        flushPending();
        String json = ShareUtils.transfer(this.blockEntity.getTransformData());
        this.minecraft.setScreen(new com.yuushya.modelling.gui.showblock.EditScreen(this,
                Component.translatable("gui.showBlockScreen.workshop.save"),
                Component.translatable("gui.showBlockScreen.workshop.save.tip"),
                name -> {
                    if (name != null && !name.isBlank()) {
                        try {
                            com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader.saveBlock(json, name);
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

    /** 用副手物品新增（副手不是方块时给提示）。 */
    private void addFromOffhand() {
        if (this.minecraft.player == null) {
            return;
        }
        ItemStack offhand = this.minecraft.player.getOffhandItem();
        if (offhand.isEmpty() || stateOf(offhand) == null || stateOf(offhand).isAir()) {
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
        if (stack.isEmpty()) {
            return;
        }
        BlockState state = stateOf(stack);
        if (state == null || state.isAir()) {
            return;
        }
        this.properties.commitAll();
        flushPending();

        int target;
        TransformBlockData data;
        boolean reuse = this.slot >= 0 && this.slot < this.blockEntity.size()
                && ModelingElementList.isEmpty(this.blockEntity.getTransformData(this.slot));
        if (reuse) {
            target = this.slot;
            data = this.blockEntity.getTransformData(target);
            data.set(new org.joml.Vector3d(0, 0, 0), new org.joml.Vector3f(0, 0, 0),
                    new org.joml.Vector3f(1, 1, 1), state, true);
        } else {
            data = new TransformBlockData();
            data.blockState = state;
            data.isShown = true;
            this.blockEntity.addTransformData(data);
            target = this.blockEntity.size() - 1;
        }

        pushElement(target, data);
        this.slot = target;
        this.blockEntity.setSlot(target);
        this.property = null;
        this.properties.refresh(target);
        this.elementList.setSelected(target);
        this.elementList.markDirty();
        this.viewport.invalidate();
    }

    /** 物品 → 方块状态：方块物品取其状态（含 BLOCK_STATE 组件），否则看模组自带的 BLOCKSTATE 组件。 */
    private static BlockState stateOf(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            net.minecraft.world.item.component.BlockItemStateProperties props =
                    stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            return props == null ? state : props.apply(state);
        }
        var holder = com.yuushya.modelling.registries.DataComponentRegistry.BLOCKSTATE;
        return holder.isBound() ? stack.get(holder.get()) : null;
    }

    /** 复制当前元素，原样重叠并选中副本。 */
    private void duplicateElement() {
        if (this.blockEntity.size() <= 0) {
            return;
        }
        this.properties.commitAll();
        flushPending();

        TransformBlockData source = this.blockEntity.getTransformData(this.slot);
        TransformBlockData copy = new TransformBlockData(source.pos, source.rot, source.scales,
                source.blockState, source.isShown);
        this.blockEntity.addTransformData(copy);
        int newSlot = this.blockEntity.size() - 1;

        pushElement(newSlot, copy);
        this.slot = newSlot;
        this.blockEntity.setSlot(newSlot);
        this.property = null;
        this.properties.refresh(newSlot);
        this.elementList.setSelected(newSlot);
        this.viewport.invalidate();
    }

    /**
     * 删除当前元素：真正从列表移除，后续元素整体前移，并重新下发受影响的槽位。
     * <p>
     * 服务端只有「把某槽位设为空」的语义（{@code REMOVE}），没有收缩数组的操作，
     * 因此这里在客户端收缩后，必须把从被删位置起的每个槽位整体重发一遍。
     */
    private void removeElement() {
        int size = this.blockEntity.size();
        if (size <= 0) {
            return;
        }
        this.properties.commitAll();
        flushPending();

        int removedAt = this.slot;
        this.blockEntity.removeTransformDataAt(removedAt);
        if (this.blockEntity.size() <= 0) {
            // 至少保留一个空槽位，否则界面无从显示
            this.blockEntity.addTransformData(new TransformBlockData());
        }

        // 从被删位置起，把后续元素整体重发到前移后的槽位
        for (int i = removedAt; i < this.blockEntity.size(); i++) {
            pushElement(i, this.blockEntity.getTransformData(i));
        }
        // 原末尾多出来的槽位清空
        for (int i = this.blockEntity.size(); i < size; i++) {
            TransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), i, TransformType.REMOVE, 0.0);
        }

        int newSlot = Math.min(removedAt, this.blockEntity.size() - 1);
        this.slot = newSlot;
        this.blockEntity.setSlot(newSlot);
        this.property = null;
        this.properties.refresh(newSlot);
        this.elementList.setSelected(newSlot);
        this.elementList.markDirty();
        this.blockEntity.getLevel().sendBlockUpdated(this.blockEntity.getBlockPos(),
                this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.viewport.invalidate();
    }

    /** 切换当前元素的显示状态。 */
    private void toggleShown() {
        if (this.blockEntity.size() <= 0) {
            return;
        }
        double shown = TransformType.SHOWN.extract(this.blockEntity, this.slot) != 0.0 ? 0.0 : 1.0;
        applyValue(TransformType.SHOWN, shown);
    }

    /** 把新元素的全部字段下发给服务端。 */
    private void pushElement(int slot, TransformBlockData data) {
        BlockPos pos = this.blockEntity.getBlockPos();
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_X, data.pos.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_Y, data.pos.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_Z, data.pos.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_X, data.rot.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_Y, data.rot.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_Z, data.rot.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_X, data.scales.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_Y, data.scales.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_Z, data.scales.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.BLOCK_STATE, Block.getId(data.blockState));
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SHOWN, data.isShown ? 1 : 0);
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

    private Component currentElementName() {
        if (this.blockEntity.size() <= 0) {
            return Component.translatable("gui.yuushya.modeling.empty");
        }
        return getBlockState().getBlock().getName();
    }
}
