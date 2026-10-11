package com.yuushya.modelling.gui.itemblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.AbstractTransformScreen;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.gui.widget.ItemStackIconList;
import com.yuushya.modelling.gui.widget.TransformComponent;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.UpdateAOPacket;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import com.yuushya.modelling.utils.YuushyaUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.*;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_SPECIAL_RENDER;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.FULL_BLOCK;
import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.utils.YuushyaUtils.normalizeAngle;

public class ItemBlockScreen extends AbstractTransformScreen<ItemTransformType, ItemBlockEntity> {
    private final ItemStack newItemStack;
    private final Map<ItemTransformType, TransformComponent<ItemTransformType>> panelSize = new LinkedHashMap<>();
    public Button colorFinishButton;
    public CycleButton<ApplyColor> colorApplyButton;
    private ItemStack itemStack = ItemStack.EMPTY;
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private ItemStackIconList itemStackList;
    private Property<?> property;
    private Button leftPropertyButton;
    private Button rightPropertyButton;
    private Button leftStateButton;
    private Button rightStateButton;
    private CycleButton<Boolean> enableBlockButton;

    public ItemBlockScreen(ItemBlockEntity blockEntity, ItemStack newItemStack) {
        super(GameNarrator.NO_TITLE, blockEntity);
        this.newItemStack = newItemStack;
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    @Override
    protected TransformComponent<ItemTransformType> choose(ItemTransformType type) {
        return choose(type, TransformComponent.Style.ITEM_ROW);
    }

    private TransformComponent<ItemTransformType> chooseSize(ItemTransformType type) {
        return panelSize.computeIfAbsent(type, key -> new TransformComponent<>(key, TransformComponent.Style.SIZE_ROW));
    }

    @Override
    protected double extract(ItemTransformType type, int slot) {
        return type.extract(this.blockEntity, slot);
    }

    @Override
    protected void modify(ItemTransformType type, int slot, double number) {
        type.modify(this.blockEntity, slot, number);
    }

    @Override
    protected void sendTransform(BlockPos pos, int slot, ItemTransformType type, double number) {
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, type, number);
    }

    @Override
    protected void sendSuccess(BlockPos pos) {
        ItemTransformDataOncePacket.sendToServerSideSuccess(pos);
    }

    @Override
    protected ShareController shareController() {
        return this.share;
    }

    private final ShareController share = new ShareController() {
        @Override
        public String serialize() {
            return ShareUtils.transferItems(blockEntity.getTransformData());
        }

        @Override
        public void load(String clipboard) {
            ShareUtils.ShareItemInformation shareInformation = ShareUtils.fromItems(clipboard);
            if (shareInformation.items().isEmpty()) {
                throw new IllegalArgumentException("No item data found");
            }
            checkModLack(shareInformation);
            updateAllTransformData(shareInformation);
        }

        @Override
        public void save(String name) throws IOException {
            EngraveItemResultLoader.saveItem(ShareUtils.transferItems(blockEntity.getTransformData()), name);
        }
    };

    @Override
    protected void flushSlotExtra() {
        flushItemStack();
    }

    @Override
    protected void flushOnRemoved() {
        flushItemStack();
    }

    private void flushItemStack() {
        if (!itemStack.isEmpty()) {
            PacketDistributor.sendToServer(new ItemStackPacket(this.blockEntity.getBlockPos(), this.slot, itemStack));
        }
        this.itemStack = ItemStack.EMPTY;
    }

    @Override
    protected void onSlotChanged() {
        for (TransformComponent<ItemTransformType> size : this.panelSize.values()) {
            size.setSliderInitial(extract(size.type, slot));
        }

        double color = COLOR.extract(blockEntity, slot);
        this.colorWidget.setColor((int) color);
        this.colorEditBox.setValue(String.format("#%06X", (0xFFFFFF & (int) color)));

        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
        boolean enableBlock = ENABLE_BLOCK.extract(blockEntity, slot) == 1;
        updateBlockStateButtonVisible(updateStateButton() && enableBlock);
        enableBlockButton.setValue(enableBlock);
    }

    private void updateColorVisible(boolean visible) {
        this.colorWidget.visible = visible;
        this.colorEditBox.visible = visible;
        this.colorFinishButton.visible = visible;
        this.colorApplyButton.visible = visible;
    }

    private void updateBlockStateButtonVisible(boolean visible) {
        this.leftPropertyButton.visible = visible;
        this.rightPropertyButton.visible = visible;
        this.leftStateButton.visible = visible;
        this.rightStateButton.visible = visible;
    }

    @Nullable
    public BlockState getBlockState() {
        ItemStack itemStack = blockEntity.getTransformData(slot).itemStack;
        BlockState blockState = itemStack.get(DataComponentRegistry.BLOCKSTATE);
        if (blockState == null) {
            Item item = itemStack.getItem();
            if (item instanceof BlockItem blockItem) {
                return blockItem.getBlock().defaultBlockState();
            }
        }
        return blockState;
    }

    @Nullable
    public Collection<Property<?>> getProperties() {
        BlockState blockState = getBlockState();
        if (blockState == null) {return null;}
        return blockState.getProperties();
    }

    public boolean updateStateButton() {
        BlockState blockState = getBlockState();
        property = null;
        if (blockState == null) {
            return false;
        }
        Collection<Property<?>> collection = blockState.getBlock().getStateDefinition().getProperties();
        boolean stateButtonVisible = !collection.isEmpty();
        if (stateButtonVisible) property = collection.iterator().next();
        return stateButtonVisible;
    }

    public ItemStack getItemStack() {
        return blockEntity.getTransformData(slot).itemStack;
    }

    @Override
    protected void init() {
        if (minecraft == null) return;

        Button addItemButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            if (this.newItemStack != null) {
                                if (this.newItemStack.isEmpty()) return;
                                if (newItemStack.getItem() instanceof BlockItem blockItem) {
                                    newItemStack.set(DataComponentRegistry.BLOCKSTATE, blockItem.getBlock().defaultBlockState());
                                }
                                itemStackList.addSlot();
                                updateItemStack(newItemStack);
                                updateTransformDataClient(SHOWN, 1.0);
                                updateStateButton();
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button copyItemButton = Button.builder(Component.literal("⧉"),
                        (btn) -> {
                            ItemStackIconList.Entry selected = this.itemStackList.getSelected();
                            if (selected != null) {
                                itemStackList.addSlot();
                                updateTransformDataClient(selected.getTransformData());
                                itemStackList.setSelectedSlot(slot);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button removeItemButton = Button.builder(Component.literal("×"),
                        (btn) -> updateTransformDataClient(REMOVE, 0.0)
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 2, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button replaceButton = Button.builder(Component.literal("⇄"),
                        (btn) -> {
                            int chosen = this.itemStackList.getChosenOne();
                            if (chosen != -1 && chosen != slot) {
                                ItemStack item = blockEntity.getTransformData(chosen).itemStack;
                                if (item == null) return;
                                if (item.isEmpty()) return;
                                updateItemStack(item);
                            } else if (this.newItemStack != null) {
                                if (this.newItemStack.isEmpty()) return;
                                updateItemStack(newItemStack);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.replace")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = createShownButton(SHOWN, RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4);

        Button copyButton = createCopyButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6);
        Button parseButton = createPasteButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 7);
        Button saveButton = createSaveButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 8);

        CycleButton<Boolean> ambientOcclusionButton = CycleButton
                .booleanBuilder(Component.literal("●"), Component.literal("☀"))
                .displayOnlyValue()
                .withInitialValue(blockEntity.getBlockState().getValue(ENABLE_AO))
                .withTooltip((on -> Tooltip.create(on ? Component.translatable("gui.itemBlockScreen.ambientOcclusion.on") : Component.translatable("gui.itemBlockScreen.ambientOcclusion.off"))))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 9, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, enableAO) -> {
                            Level level = blockEntity.getLevel();
                            if (level != null) {
                                PacketDistributor.sendToServer(new UpdateAOPacket(enableAO, blockEntity.getBlockState().getValue(FULL_BLOCK), blockEntity.getBlockState().getValue(ENABLE_SPECIAL_RENDER), blockEntity.getBlockPos()));
                            }
                        });

        CycleButton<Boolean> fullBlockButton = CycleButton
                .booleanBuilder(Component.literal("■"), Component.literal("□"))
                .displayOnlyValue()
                .withInitialValue(blockEntity.getBlockState().getValue(FULL_BLOCK))
                .withTooltip((on -> Tooltip.create(on ? Component.translatable("gui.itemBlockScreen.fullBlock.on") : Component.translatable("gui.itemBlockScreen.fullBlock.off"))))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 10, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, fullBlock) -> {
                            Level level = blockEntity.getLevel();
                            if (level != null) {
                                PacketDistributor.sendToServer(new UpdateAOPacket(blockEntity.getBlockState().getValue(ENABLE_AO), fullBlock, blockEntity.getBlockState().getValue(ENABLE_SPECIAL_RENDER), blockEntity.getBlockPos()));
                            }
                        });

        CycleButton<Boolean> enableSpecialRenderButton = CycleButton
                .booleanBuilder(Component.literal("★"), Component.literal("☆"))
                .displayOnlyValue()
                .withInitialValue(blockEntity.getBlockState().getValue(ENABLE_SPECIAL_RENDER))
                .withTooltip((on -> Tooltip.create(on ? Component.translatable("gui.itemBlockScreen.enableSpecialRender.on") : Component.translatable("gui.itemBlockScreen.enableSpecialRender.off"))))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 11, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, enableSpecialRender) -> {
                            Level level = blockEntity.getLevel();
                            if (level != null) {
                                PacketDistributor.sendToServer(new UpdateAOPacket(blockEntity.getBlockState().getValue(ENABLE_AO), blockEntity.getBlockState().getValue(FULL_BLOCK), enableSpecialRender, blockEntity.getBlockPos()));
                            }
                        });

        Button xMirror = Button.builder(Component.literal("x"), (button -> mirror(YuushyaUtils.MirrorFace.X)))
                .bounds(RIGHT_COLUMN_X, RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT + 5, RIGHT_BAR_WIDTH, PER_HEIGHT).tooltip(Tooltip.create(Component.translatable("gui.itemBlockScreen.mirror.tip", "X"))).build();

        Button yMirror = Button.builder(Component.literal("y"), (button -> mirror(YuushyaUtils.MirrorFace.Y)))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT + 5, RIGHT_BAR_WIDTH, PER_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.itemBlockScreen.mirror.tip", "Y"))).build();

        Button zMirror = Button.builder(Component.literal("z"), (button -> mirror(YuushyaUtils.MirrorFace.Z)))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 2, RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT + 5, RIGHT_BAR_WIDTH, PER_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.itemBlockScreen.mirror.tip", "Z"))).build();

        itemStackList = new ItemStackIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        CycleButton<BlockShape> shapeButton = createShapeButton(SHAPE, SHAPE.extractShape(blockEntity));

        modeButton = createModeButton(Mode.values(), Mode.SLIDER, (btn, mode) -> {
            switch (mode) {
                case SLIDER -> applySliderMode(false, true);
                case FINE_TUNE -> applySliderMode(true, true);
                case EDIT -> applySliderMode(true, false);
                case COLOR, CUSTOM_SIZE -> panel.values().forEach(TransformComponent::setInvisible);
            }
            switch (mode) {
                case CUSTOM_SIZE -> panelSize.values().forEach((it) -> it.triggerVisible(true));
                default -> panelSize.values().forEach((it) -> it.triggerVisible(false));
            }
            updateColorVisible(mode == Mode.COLOR);
        });

        // Setup transform components for items
        choose(SCALE_X); // 首先放置scala_x, 因为pos_x依赖于它
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x()), Math.abs(blockEntity.getTransformData(slot).pos.x()));
        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y()), Math.abs(blockEntity.getTransformData(slot).pos.y()));
        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z()), Math.abs(blockEntity.getTransformData(slot).pos.z()));
        addTransformRows(List.of(POS_X, POS_Y, POS_Z), List.of(ROT_X, ROT_Y, ROT_Z),
                "gui.yuushya.itemBlockScreen.", "block.yuushya.itemblock.", posX, posY, posZ);

        addUniformScaleRow(List.of(SCALE_X, SCALE_Y, SCALE_Z), List.of(POS_X, POS_Y, POS_Z),
                Component.empty(), "gui.yuushya.itemBlockScreen.scale_text", 6, 10.0);
        addBrightnessRow(LIT, Component.translatable("gui.yuushya.itemBlockScreen.brightness_text"), 7);

        addSizeRow(panelSize, SCALE_X, POS_X, 0, Component.empty(), "gui.yuushya.itemBlockScreen.scale_text", 10.0);
        addSizeRow(panelSize, SCALE_Y, POS_Y, 2, Component.empty(), "gui.yuushya.itemBlockScreen.scale_text", 10.0);
        addSizeRow(panelSize, SCALE_Z, POS_Z, 4, Component.empty(), "gui.yuushya.itemBlockScreen.scale_text", 10.0);

        boolean enableBlock = ENABLE_BLOCK.extract(blockEntity, slot) == 1;
        enableBlockButton = CycleButton.booleanBuilder(Component.translatable("gui.textBlockScreen.block"), Component.translatable("gui.textBlockScreen.item"))
                .displayOnlyValue()
                .withInitialValue(enableBlock)
                .create(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH , TOP + PER_HEIGHT + PER_HEIGHT, RIGHT_LIST_WIDTH, PER_HEIGHT, Component.empty(), (button, bool) -> {
                    updateBlockStateButtonVisible(bool);
                    updateTransformDataClient(ENABLE_BLOCK, bool ? 1.0 : 0.0);
                });

        leftPropertyButton = Button.builder(Component.literal("<"),
                        (btn) -> {
                            if (property != null) {
                                property = YuushyaBlockStates.getRelative(getProperties(), property, true);
                            }
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH , TOP + PER_HEIGHT + PER_HEIGHT * 2, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        rightPropertyButton = Button.builder(Component.literal(">"),
                        (btn) -> {
                            if (property != null) {
                                property = YuushyaBlockStates.getRelative(getProperties(), property, false);
                            }
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH * 5 / 2 , TOP + PER_HEIGHT + PER_HEIGHT * 2, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        leftStateButton = Button.builder(Component.literal("<"),
                        (btn) -> {
                            BlockState blockState = getBlockState();
                            if (blockState == null) return;
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(blockState, property, true);
                            ItemStack itemStack = blockEntity.getTransformData(slot).itemStack;
                            itemStack.set(DataComponentRegistry.BLOCKSTATE, nextBlockState);
                            updateItemStack(itemStack);
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH , TOP + PER_HEIGHT + PER_HEIGHT * 3, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        rightStateButton = Button.builder(Component.literal(">"),
                        (btn) -> {
                            BlockState blockState = getBlockState();
                            if (blockState == null) return;
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, false);
                            ItemStack itemStack = blockEntity.getTransformData(slot).itemStack;
                            itemStack.set(DataComponentRegistry.BLOCKSTATE, nextBlockState);
                            updateItemStack(itemStack);
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH * 5 / 2 , TOP + PER_HEIGHT + PER_HEIGHT * 3, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();

        updateBlockStateButtonVisible(updateStateButton() && enableBlock);

        this.colorWidget = new ColorWidget(leftColumnX() - 10, top(-2, 30), 110, 160, (int) COLOR.extract(blockEntity, slot), Component.translatable("gui.yuushya.itemBlockScreen.color_text"), this);
        this.colorEditBox = new EditBox(this.font, leftColumnX() - 5, top(6, 30), leftColumnWidth(), PER_HEIGHT, Component.translatable("gui.yuushya.itemBlockScreen.color_text"));
        this.colorEditBox.setMaxLength(7);
        this.colorFinishButton = Button.builder(Component.literal("√").withStyle(ChatFormatting.GREEN), (button -> {
            String text = colorEditBox.getValue();
            if (text.startsWith("#")) {
                try {
                    int color = Integer.parseInt(text.substring(1), 16);
                    colorWidget.setColor(color);
                    updateTransformDataClient(COLOR, (double) color);
                } catch (NumberFormatException ignored) {
                    Yuushya.LOGGER.error("Invalid color number");
                }
            }
        })).bounds(leftColumnX() + leftColumnWidth() - 5, top(6, 30), SMALL_BUTTON_WIDTH, PER_HEIGHT).build();

        colorApplyButton = CycleButton.builder(ApplyColor::getSymbol)
                .displayOnlyValue()
                .withValues(ApplyColor.values())
                .withInitialValue(ApplyColor.PRE_APPLY)
                .withTooltip((mode) -> Tooltip.create(mode.getDescription()))
                .create(leftColumnX() + 30, top(-2, 30) + 5, 50, PER_HEIGHT, Component.literal("TYPE"),
                        (button, mode) -> {
                            switch (mode) {
                                case PRE_APPLY, APPLY -> {
                                }
                                case DONE -> {
                                    int color = (int) COLOR.extract(blockEntity, slot);
                                    for (int i = 0; i < blockEntity.getTransformData().size(); i++) {
                                        if (i == slot) continue;
                                        updateTransformDataSever(COLOR, (double) color, i);
                                    }
                                    this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
                                    this.colorApplyButton.setValue(ApplyColor.PRE_APPLY);
                                }
                            }
                        }
                );

        this.colorWidget.visible = false;
        this.colorEditBox.visible = false;
        this.colorFinishButton.visible = false;
        this.colorApplyButton.visible = false;

        registerPanelWidgets();

        for (TransformComponent<ItemTransformType> size : this.panelSize.values()) {
            size.initWidget(this.font);
            this.addRenderableWidget(size.sliderButton);
            this.addRenderableWidget(size.minusButton);
            this.addRenderableWidget(size.addButton);
            this.addRenderableWidget(size.editBox);
            this.addRenderableWidget(size.cancelButton);
            this.addRenderableWidget(size.finishButton);
            size.triggerVisible(false);
        }

        this.addRenderableWidget(modeButton);
        this.addRenderableWidget(shapeButton);
        this.addWidget(this.itemStackList);
        this.addRenderableWidget(addItemButton);
        this.addRenderableWidget(removeItemButton);
        this.addRenderableWidget(copyItemButton);
        this.addRenderableWidget(replaceButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(parseButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(ambientOcclusionButton);
        this.addRenderableWidget(fullBlockButton);
        this.addRenderableWidget(enableSpecialRenderButton);
        this.addRenderableWidget(xMirror);
        this.addRenderableWidget(yMirror);
        this.addRenderableWidget(zMirror);
        this.addRenderableWidget(colorWidget);
        this.addRenderableWidget(colorEditBox);
        this.addRenderableWidget(colorFinishButton);
        this.addRenderableWidget(colorApplyButton);
        this.addRenderableWidget(enableBlockButton);
        this.addRenderableWidget(leftPropertyButton);
        this.addRenderableWidget(rightStateButton);
        this.addRenderableWidget(leftStateButton);
        this.addRenderableWidget(rightPropertyButton);

        itemStackList.setSelectedSlot(slot);
    }

    private void mirror(YuushyaUtils.MirrorFace face) {
        double x = POS_X.extract(blockEntity, slot);
        double y = POS_Y.extract(blockEntity, slot);
        double z = POS_Z.extract(blockEntity, slot);
        Vector3d pos = new Vector3d(x, y, z);

        double xRot = ROT_X.extract(blockEntity, slot);
        double yRot = ROT_Y.extract(blockEntity, slot);
        double zRot = ROT_Z.extract(blockEntity, slot);
        Quaternionf rot = new Quaternionf().rotateXYZ((float) Math.toRadians(xRot), (float) Math.toRadians(yRot), (float) Math.toRadians(zRot));

        YuushyaUtils.mirror(pos, rot, face);

        updateTransformDataClient(POS_X, pos.x);
        updateTransformDataClient(POS_Y, pos.y);
        updateTransformDataClient(POS_Z, pos.z);

        Vector3f euler = rot.getEulerAnglesXYZ(new Vector3f());
        updateTransformDataClient(ROT_X, normalizeAngle(Math.toDegrees(euler.x)));
        updateTransformDataClient(ROT_Y, normalizeAngle(Math.toDegrees(euler.y)));
        updateTransformDataClient(ROT_Z, normalizeAngle(Math.toDegrees(euler.z)));

        this.itemStackList.setSelectedSlot(slot);
    }

    @Override
    protected boolean isEditMode() {
        return modeButton.getValue() == Mode.EDIT;
    }

    @Override
    protected void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.itemStackList.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderHeader(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, getItemStack().getDisplayName(), RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT, 0xFFFFFFFF, false);
    }

    @Override
    protected void renderFooter(GuiGraphics guiGraphics) {
        if (ENABLE_BLOCK.extract(blockEntity, slot) == 1) {
            BlockState blockState = getBlockState();
            if (blockState != null && property != null) {
                String propertyName = property.getName();
                guiGraphics.drawString(this.font, propertyName, RIGHT_COLUMN_X + RIGHT_LIST_WIDTH * 2 - font.width(propertyName) / 2 , TOP + PER_HEIGHT + PER_HEIGHT * 2 + 5, 0xFFFFFFFF, false);
                String propertyValue = YuushyaDebugStickItem.getNameHelper(blockState, property);
                guiGraphics.drawString(this.font, propertyValue, RIGHT_COLUMN_X + RIGHT_LIST_WIDTH * 2 - font.width(propertyValue) / 2 , TOP + PER_HEIGHT + PER_HEIGHT * 3 + 5, 0xFFFFFFFF, false);
            }
        }
    }

    public void checkModLack(ShareUtils.ShareItemInformation shareInformation) {
        List<String> unLoaded = shareInformation.mods().stream().filter(id -> !ModList.get().isLoaded(id)).toList();
        if (unLoaded.isEmpty()) return;
        if (unLoaded.contains("yuushya")) return;
        Minecraft.getInstance().getToasts().addToast(
                SystemToast.multiline(Minecraft.getInstance(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.literal("Mod Lack"), Component.literal(String.join(", ", unLoaded)))
        );
    }

    private void updateAllTransformData(ShareUtils.ShareItemInformation shareInformation) {
        List<TransformItemData> dataList = blockEntity.getTransformData();
        BlockPos pos = blockEntity.getBlockPos();
        int currentSize = dataList.size();
        for (int slot = 0; slot < currentSize; slot++) {
            blockEntity.removeTransformData(slot);
            ItemTransformDataOncePacket.sendToServerSide(pos, slot, REMOVE, 0.0);
        }

        shareInformation.transferItems(dataList);

        int nextSize = dataList.size();
        this.blockEntity.getLevel().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
        this.storage.clear();
        for (int slot = 0; slot < nextSize; slot++) {
            TransformItemData data = dataList.get(slot);
            updateTransformDataServerImmediate(data, slot);
        }
        ItemTransformDataOncePacket.sendToServerSideSuccess(pos);
        for (int slot = nextSize - 1; slot < currentSize; slot++) {
            blockEntity.setSlot(slot);
        }
        boolean enableBlock = ENABLE_BLOCK.extract(blockEntity, slot) == 1;
        updateBlockStateButtonVisible(updateStateButton() && enableBlock);
        enableBlockButton.setValue(enableBlock);
        this.itemStackList.updateRenderList();
    }

    private void updateTransformDataServerImmediate(TransformItemData data, int slot) {
        if (data.itemStack.isEmpty()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_X, data.pos.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_Y, data.pos.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_Z, data.pos.z);

        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_X, data.rot.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Y, data.rot.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Z, data.rot.z);

        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_X, data.scales.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Y, data.scales.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Z, data.scales.z);

        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SHOWN, data.isShown ? 1 : 0);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ENABLE_BLOCK, data.enableBlock ? 1 : 0);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, COLOR, data.color);

        PacketDistributor.sendToServer(new ItemStackPacket(pos, slot, data.itemStack));

    }

    private void updateTransformDataClient(TransformItemData data) {
        updateTransformDataClient(POS_X, data.pos.x);
        updateTransformDataClient(POS_Y, data.pos.y);
        updateTransformDataClient(POS_Z, data.pos.z);

        updateTransformDataClient(ROT_X, (double) data.rot.x);
        updateTransformDataClient(ROT_Y, (double) data.rot.y);
        updateTransformDataClient(ROT_Z, (double) data.rot.z);

        updateTransformDataClient(SCALE_X, (double) data.scales.x);
        updateTransformDataClient(SCALE_Y, (double) data.scales.y);
        updateTransformDataClient(SCALE_Z, (double) data.scales.z);

        updateTransformDataClient(SHOWN, data.isShown ? 1.0 : 0.0);
        updateTransformDataClient(ENABLE_BLOCK, data.enableBlock ? 1.0 : 0.0);
        updateTransformDataClient(COLOR, (double) data.color);

        updateItemStack(data.itemStack);
    }

    public void updateTransformDataSever(ItemTransformType type, Double number, int slot) {
        type.modify(blockEntity, slot, number);
        ItemTransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), slot, type, number);
    }

    private void updateItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
        ITEM_STACK.modify(blockEntity, slot, itemStack);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public void updateColorData(int colorValue) {
        updateTransformDataClient(ItemTransformType.COLOR, (double) colorValue);
    }

    public Font getFont() {
        return this.font;
    }

    public enum Mode implements ScreenMode {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit"), COLOR("color"), CUSTOM_SIZE("custom_size");

        private final String name;
        @Getter
        private final Component symbol;

        Mode(String name) {
            this.name = name;
            this.symbol = Component.translatable("gui.itemBlockScreen.mode." + name);
        }

        @Override
        public Component getTooltip() {
            return switch (this) {
                case COLOR -> Component.translatable("gui.showBlockScreen.mode.color.tooltip");
                default -> Component.translatable("gui.itemBlockScreen.mode." + name + ".tooltip");
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    public enum ApplyColor {
        PRE_APPLY,
        APPLY,
        DONE;

        public Component getSymbol() {
            return Component.translatable("gui.itemBlockScreen.apply." + name().toLowerCase());
        }

        public Component getDescription() {
            return Component.translatable("gui.itemBlockScreen.apply.tip." + name().toLowerCase());
        }
    }
}