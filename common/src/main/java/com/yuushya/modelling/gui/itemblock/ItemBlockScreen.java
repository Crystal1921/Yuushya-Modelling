package com.yuushya.modelling.gui.itemblock;

import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.showblock.EditScreen;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.widget.ColorWidget;
import com.yuushya.modelling.gui.widget.ItemStackIconList;
import com.yuushya.modelling.gui.widget.ItemTransformComponent;
import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;

public class ItemBlockScreen extends Screen {
    public static final int PER_HEIGHT = 20;
    public static final int SMALL_BUTTON_WIDTH = 10;
    private static final int TOP = 10;
    private static final int RIGHT_COLUMN_X = 2;
    private static final int RIGHT_BAR_WIDTH = PER_HEIGHT;
    private static final int RIGHT_LIST_WIDTH = 40;
    private static final int RIGHT_LIST_PER_HEIGHT = 45;
    private static final int RIGHT_LIST_TOP = TOP + PER_HEIGHT + 5;
    private static final int RIGHT_LIST_HEIGHT = 3 * RIGHT_LIST_PER_HEIGHT + 2;
    private static final int RIGHT_LIST_BOTTOM = RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT;
    private static final int RIGHT_STATE_PANEL_Y = RIGHT_LIST_BOTTOM + 5;
    private static final int RIGHT_STATE_INFORM_X = RIGHT_COLUMN_X + RIGHT_LIST_WIDTH + 3;

    private final ItemBlockEntity blockEntity;
    private final ItemStack newItemStack;
    private final Map<ItemTransformType, Double> storage = new HashMap<>();
    private final Map<ItemTransformType, ItemTransformComponent> panel = new LinkedHashMap<>();
    private final Map<ItemTransformType, EditBox> editBoxes = new HashMap<>();
    private int slot;
    private ItemStack itemStack = ItemStack.EMPTY;
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private ColorWidget colorWidget;
    private ItemStackIconList itemStackList;
    private Button leftItemButton;
    private Button rightItemButton;

    public ItemBlockScreen(ItemBlockEntity blockEntity, ItemStack newItemStack) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
        this.newItemStack = newItemStack;
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    // i \in [1,...]
    private static int top(int i, int offset) {
        return TOP + PER_HEIGHT + 10 + PER_HEIGHT * i + offset;
    }

    public void setSlot(int slot) {
        for (ItemTransformType key : this.storage.keySet()) {
            ItemTransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, key, this.storage.get(key));
        }

        if (!itemStack.isEmpty()) {
            NetworkManager.sendToServer(new ItemStackPacket(this.blockEntity.getBlockPos(), this.slot, itemStack));
        }

        this.itemStack = ItemStack.EMPTY;
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (ItemTransformComponent component : this.panel.values()) {
            component.setSliderInitial(this.blockEntity, this.slot);
        }
        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
        updateItemButtonVisible(true);
    }

    private ItemTransformComponent choose(ItemTransformType type) {
        return panel.computeIfAbsent(type, ItemTransformComponent::new);
    }

    public boolean updateItemButtonVisible(boolean force) {
        ItemStack itemStack = getItemStack();
        boolean itemButtonVisible = !itemStack.isEmpty();
        leftItemButton.visible = itemButtonVisible;
        rightItemButton.visible = itemButtonVisible;
        return itemButtonVisible;
    }

    public ItemStack getItemStack() {
        return blockEntity.getTransformData(slot).itemStack;
    }

    private int leftColumnX() {
        return this.width / 4 * 3 + 10;
    }

    private int leftColumnWidth() {
        return this.width / 4 - 20;
    }

    @Override
    protected void init() {
        if (minecraft == null) return;

        Button addItemButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            int chosen = this.itemStackList.getChosenOne();
                            if (chosen != -1) {
                                itemStackList.addSlot();
                                blockEntity.getTransformData().add(new TransformItemData());
                                updateTransformDataServerImmediate(blockEntity.getTransformData(chosen), slot);
                                ItemTransformDataOncePacket.sendToServerSideSuccess(blockEntity.getBlockPos());
                                updateItemButtonVisible(true);
                            } else if (this.newItemStack != null) {
                                itemStackList.addSlot();
                                updateItemStack(newItemStack);
                                updateTransformData(SHOWN, 1.0);
                                updateItemButtonVisible(true);
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button removeItemButton = Button.builder(Component.literal("×"),
                        (btn) -> {
                            updateTransformData(REMOVE, 0.0);
                            updateItemButtonVisible(true);
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button replaceButton = Button.builder(Component.literal("⇄"),
                        (btn) -> {
                            int chosen = this.itemStackList.getChosenOne();
                            if (chosen != -1 && chosen != slot) {
                                updateItemStack(blockEntity.getTransformData(chosen).itemStack);
                                updateItemButtonVisible(true);
                            } else if (this.newItemStack != null) {
                                updateItemStack(newItemStack);
                                updateItemButtonVisible(true);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.replace")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = CycleButton.booleanBuilder(
                        Component.literal("🕶"),//Component.translatable("gui.itemBlockScreen.display.on"),
                        Component.literal("👀"))//Component.translatable("gui.itemBlockScreen.display.off"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformData(SHOWN, bl ? 1.0 : 0.0)
                );

        Button copyButton = Button.builder(Component.literal("\uD83D\uDCE4").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            String res = ShareUtils.transferItems(blockEntity.getTransformData());
                            setClipboard(res);
                            this.minecraft.getToasts().addToast(
                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.copy_pass"), Component.translatable("gui.showBlockScreen.workshop.share_hint"))
                            );
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3 + 40, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button parseButton = Button.builder(Component.literal("\uD83D\uDCE5").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            String string = getClipboard();
                            try {
                                ShareUtils.ShareItemInformation shareInformation = ShareUtils.fromItems(string);
                                checkModLack(shareInformation);
                                updateAllTransformData(shareInformation);
                                updateItemButtonVisible(true);
                                this.minecraft.getToasts().addToast(
                                        new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.paste_pass"), null)
                                );
                            } catch (Exception e) {
                                this.minecraft.getToasts().addToast(
                                        SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop.error"), Component.literal(e.getMessage()))
                                );
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.paste")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3 + 60, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button saveButton = Button.builder(Component.literal("\uD83D\uDCBE").withStyle(ChatFormatting.BOLD),
                        (btn) -> this.minecraft.setScreen(new EditScreen(this,
                                Component.translatable("gui.showBlockScreen.workshop.save"),
                                Component.translatable("gui.showBlockScreen.workshop.save.tip"),
                                (string) -> {
                                    if (string != null) {
                                        String res = ShareUtils.transferItems(blockEntity.getTransformData());
                                        try {
                                            EngraveItemResultLoader.saveItem(res, string);
                                            this.minecraft.getToasts().addToast(
                                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.save_pass"), Component.translatable("gui.showBlockScreen.workshop.share_hint"))
                                            );
                                        } catch (IOException e) {
                                            this.minecraft.getToasts().addToast(
                                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop.save_error"), Component.literal(e.getMessage()))
                                            );
                                        }
                                        this.minecraft.setScreen(this);
                                    } else {
                                        this.minecraft.setScreen(this);
                                    }
                                },
                                (string) -> true
                        ))
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.save")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4 + 60, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        itemStackList = new ItemStackIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        leftItemButton = Button.builder(Component.literal("<"),
                        (btn) -> {
                            // Could implement item cycling here if needed
                        })
                .bounds(RIGHT_COLUMN_X, RIGHT_STATE_PANEL_Y, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();

        rightItemButton = Button.builder(Component.literal(">"),
                        (btn) -> {
                            // Could implement item cycling here if needed
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2 * 3, RIGHT_STATE_PANEL_Y, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();

        CycleButton<BlockShape> shapeButton = CycleButton.builder(BlockShape::getSymbol)
                .displayOnlyValue()
                .withValues(BlockShape.values())
                .withInitialValue(SHAPE.extractShape(blockEntity))
                .create(leftColumnX() - 50, TOP, 40, PER_HEIGHT, Component.literal("shape"),
                        (button, shape) -> updateTransformData(SHAPE, (double) shape.ordinal()));

        modeButton = CycleButton.builder(Mode::getSymbol)
                .displayOnlyValue()
                .withValues(Mode.values())
                .withInitialValue(Mode.SLIDER)
                .withTooltip((mode) -> Tooltip.create(
                        switch (mode) {
                            case SLIDER -> Component.translatable("gui.itemBlockScreen.mode.slider.tooltip");
                            case FINE_TUNE -> Component.translatable("gui.itemBlockScreen.mode.fine_tune.tooltip");
                            case EDIT -> Component.translatable("gui.itemBlockScreen.mode.edit.tooltip");
                            case COLOR -> Component.translatable("gui.showBlockScreen.mode.color.tooltip");
                        }
                ))
                .create(leftColumnX(), TOP, leftColumnWidth(), PER_HEIGHT, Component.literal("MODE"),
                        (btn, mode) -> {
                            switch (mode) {
                                case SLIDER -> panel.values().forEach(ItemTransformComponent::setSliderStep);
                                case EDIT, FINE_TUNE ->
                                        panel.values().forEach(ItemTransformComponent::setSliderFineTune);
                            }
                            switch (mode) {
                                case SLIDER, FINE_TUNE -> {
                                    panel.values().forEach((it) -> it.triggerVisible(true));
                                    this.colorWidget.visible = false;
                                }
                                case EDIT -> {
                                    panel.values().forEach((it) -> it.triggerVisible(false));
                                    this.colorWidget.visible = false;
                                }
                                case COLOR -> {
                                    panel.values().forEach(ItemTransformComponent::triggerColor);
                                    this.colorWidget.visible = true;
                                    this.setFocused(colorWidget);
                                }
                            }
                        }
                );

        // Setup transform components for items
        choose(SCALE_X); // 首先放置scala_x, 因为pos_x依赖于它
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x()), Math.abs(blockEntity.getTransformData(slot).pos.x()));
        choose(POS_X).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.pos_text"),
                                () -> -posX,
                                () -> posX,
                                () -> getStep(posX),
                                (number) -> updateTransformData(POS_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(POS_X).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.X);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(0, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y()), Math.abs(blockEntity.getTransformData(slot).pos.y()));
        choose(POS_Y).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.pos_text"),
                                () -> -posY,
                                () -> posY,
                                () -> getStep(posY),
                                (number) -> updateTransformData(POS_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(POS_Y).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Y);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(1, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z()), Math.abs(blockEntity.getTransformData(slot).pos.z()));
        choose(POS_Z).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.pos_text"),
                                () -> -posZ,
                                () -> posZ,
                                () -> getStep(posZ),
                                (number) -> updateTransformData(POS_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
                        .step(choose(POS_Z).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Z);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(2, 0), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_X).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(ROT_X).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.X);
                            blockEntity.setShowRotAxis();
                        })
                        .initial(ROT_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(3, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Y).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(ROT_Y).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Y);
                            blockEntity.setShowRotAxis();
                        })
                        .initial(ROT_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(4, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Z).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
                        .step(choose(ROT_Z).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Z);
                            blockEntity.setShowRotAxis();
                        })
                        .initial(ROT_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(5, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(SCALE_X).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(), 0.0, 1.0, 10.0,
                                (number) -> {
                                    updateTransformData(SCALE_X, number);
                                    updateTransformData(SCALE_Y, number);
                                    updateTransformData(SCALE_Z, number);
                                    choose(POS_X).sliderButton.setValidatedValue(choose(POS_X).sliderButton.getValidatedValue());
                                    choose(POS_Y).sliderButton.setValidatedValue(choose(POS_Y).sliderButton.getValidatedValue());
                                    choose(POS_Z).sliderButton.setValidatedValue(choose(POS_Z).sliderButton.getValidatedValue());
                                })
                        .text((caption, number) -> Component.translatable("gui.yuushya.itemBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(choose(SCALE_X).setStandardStep(0.1))
                        .initial(SCALE_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(6, 20), leftColumnWidth(), PER_HEIGHT).build();

        choose(LIT).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.itemBlockScreen.brightness_text"), 0.0, 15.0,
                                (number) -> updateTransformData(LIT, number))
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(LIT).setStandardStep(1))
                        .initial(LIT.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(7, 30), leftColumnWidth(), PER_HEIGHT).build();

        this.colorWidget = new ColorWidget(leftColumnX(), top(-1, 30), 100, 180, (int) COLOR.extract(blockEntity, slot), Component.translatable("gui.yuushya.itemBlockScreen.color_text"), this);
        this.colorWidget.visible = false;

        for (ItemTransformComponent component : this.panel.values()) {
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
        }
        this.addRenderableWidget(modeButton);
        this.addRenderableWidget(shapeButton);
        this.addWidget(this.itemStackList);
        this.addRenderableWidget(addItemButton);
        this.addRenderableWidget(removeItemButton);
        this.addRenderableWidget(replaceButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(leftItemButton);
        this.addRenderableWidget(rightItemButton);
        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(parseButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(colorWidget);

        itemStackList.setSelectedSlot(slot);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.itemStackList.render(guiGraphics, mouseX, mouseY, partialTick);
        ItemStack itemStack = getItemStack();
        guiGraphics.drawString(this.font, this.itemStackList.updateRenderDisplayName(itemStack), RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT, 0xFFFFFFFF, false);
        List<String> properties = this.itemStackList.updateRenderItemProperties(itemStack);
        for (int i = 0; i < properties.size(); i++) {
            MutableComponent displayItemState = Component.literal(properties.get(i));
            guiGraphics.drawString(this.font, displayItemState, RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT + this.font.lineHeight * (i + 1) + 1, 0xFFEBC6, false);
        }

        if (modeButton.getValue() == Mode.EDIT) {
            for (ItemTransformComponent component : this.panel.values()) {
                guiGraphics.drawString(this.font, component.editBox.getMessage(), component.editBox.getX() + component.editBox.getWidth() / 2, component.editBox.getY() + component.editBox.getHeight() / 3, 0x707070);
                component.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        for (ItemTransformType key : storage.keySet()) {
            ItemTransformDataOncePacket.sendToServerSide(blockEntity.getBlockPos(), slot, key, storage.get(key));
        }
        if (!itemStack.isEmpty()) {
            NetworkManager.sendToServer(new ItemStackPacket(this.blockEntity.getBlockPos(), this.slot, itemStack));
        }

        this.itemStack = ItemStack.EMPTY;
        this.storage.clear();
        ItemTransformDataOncePacket.sendToServerSideSuccess(blockEntity.getBlockPos());
    }

    public void checkModLack(ShareUtils.ShareItemInformation shareInformation) {
        List<String> unLoaded = shareInformation.mods().stream().filter(id -> !Platform.getModIds().contains(id)).toList();
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
        this.itemStackList.updateRenderList();
    }

    private void updateTransformDataServerImmediate(TransformItemData data, int slot) {
        if (data.itemStack.isEmpty()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_X, data.pos.x());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_Y, data.pos.y());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, POS_Z, data.pos.z());

        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_X, data.rot.x());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Y, data.rot.y());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Z, data.rot.z());

        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_X, data.scales.x());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Y, data.scales.y());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Z, data.scales.z());
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, SHOWN, data.isShown ? 1 : 0);

        NetworkManager.sendToServer(new ItemStackPacket(pos, slot, data.itemStack));
    }

    public void updateTransformData(ItemTransformType type, Double number) {
        this.storage.put(type, number);
        type.modify(blockEntity, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    private void updateItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
        ITEM_STACK.modify(blockEntity, slot, itemStack);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    private String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }

    private void setClipboard(String clipboardValue) {
        if (this.minecraft != null) {
            TextFieldHelper.setClipboardContents(this.minecraft, clipboardValue);
        }
    }

    public Font getFont() {
        return this.font;
    }

    public enum Mode implements StringRepresentable {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit"), COLOR("color");

        private final String name;
        @Getter
        private final Component symbol;

        Mode(String name) {
            this.name = name;
            this.symbol = Component.translatable("gui.itemBlockScreen.mode." + name);
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}