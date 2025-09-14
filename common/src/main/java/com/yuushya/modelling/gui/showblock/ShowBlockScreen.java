package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformDataNetwork;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.widget.BlockStateIconList;
import com.yuushya.modelling.gui.widget.TransformComponent;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.utils.ShareUtils;
import dev.architectury.platform.Platform;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static com.yuushya.modelling.blockentity.transformData.TransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;

public class ShowBlockScreen extends Screen {
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
    private final ShowBlockEntity blockEntity;
    private final BlockState newBlockState;
    private final Map<TransformType, Double> storage = new HashMap<>();
    private final Map<TransformType, TransformComponent> panel = new LinkedHashMap<>();
    private final Map<TransformType, EditBox> editBoxes = new HashMap<>();
    private int slot;
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private BlockStateIconList blockStateList;
    private Button leftPropertyButton;
    private Property<?> property;
    private Button rightPropertyButton;
    private Button leftStateButton;
    private Button rightStateButton;
    public ShowBlockScreen(ShowBlockEntity blockEntity, BlockState newBlockState) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
        this.newBlockState = newBlockState;
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    // i \in [1,...]
    private static int top(int i, int offset) {
        return TOP + PER_HEIGHT + 10 + PER_HEIGHT * i + offset;
    }

    public void setSlot(int slot) {
        for (TransformType key : this.storage.keySet()) {
            TransformDataNetwork.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, key, this.storage.get(key));
        }
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (TransformComponent component : this.panel.values()) {
            component.setSliderInitial(this.blockEntity, this.slot);
        }
        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
        updateStateButtonVisible(true);
    }

    private TransformComponent choose(TransformType type) {
        return panel.computeIfAbsent(type, TransformComponent::new);
    }

    public boolean updateStateButtonVisible(boolean force) {
        Collection<Property<?>> collection = blockStateList.updateRenderProperties(getBlockState());
        boolean stateButtonVisible = !collection.isEmpty();
        if (stateButtonVisible && (property == null || force)) property = collection.iterator().next();
        leftStateButton.visible = stateButtonVisible;
        rightStateButton.visible = stateButtonVisible;
        leftPropertyButton.visible = stateButtonVisible;
        rightPropertyButton.visible = stateButtonVisible;
        return stateButtonVisible;
    }

    public BlockState getBlockState() {
        return blockEntity.getTransformData(slot).blockState;
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

        Button addStateButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            int chosen = this.blockStateList.getChosenOne();
                            if (chosen != -1) {
                                blockStateList.addSlot();
                                blockEntity.getTransformData().add(new TransformBlockData());
                                updateTransformDataServerImmediate(blockEntity.getTransformData(chosen), slot);
                                TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
                                updateStateButtonVisible(true);
                            } else if (this.newBlockState != null) {
                                blockStateList.addSlot();
                                updateTransformData(BLOCK_STATE, (double) Block.getId(this.newBlockState));
                                updateTransformData(SHOWN, 1.0);
                                updateStateButtonVisible(true);
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
        Button removeStateButton = Button.builder(Component.literal("×"),
                        (btn) -> {
                            updateTransformData(REMOVE, 0.0);
                            updateStateButtonVisible(true);
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
        Button replaceButton = Button.builder(Component.literal("⇄"),
                        (btn) -> {
                            int chosen = this.blockStateList.getChosenOne();
                            if (chosen != -1 && chosen != slot) {
                                updateTransformData(BLOCK_STATE, (double) Block.getId(blockEntity.getTransformData(chosen).blockState));
                                updateStateButtonVisible(true);
                            } else {
                                updateTransformData(BLOCK_STATE, (double) Block.getId(this.newBlockState));
                                updateStateButtonVisible(true);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.replace")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = CycleButton.booleanBuilder(
                        Component.literal("🕶"),//Component.translatable("gui.showBlockScreen.display.on"),
                        Component.literal("👀"))//Component.translatable("gui.showBlockScreen.display.off"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformData(SHOWN, bl ? 1.0 : 0.0)
                );


        //Component.translatable("gui.showBlockScreen.workshop.copy"),
        Button copyButton = Button.builder(Component.literal("\uD83D\uDCE4").withStyle(ChatFormatting.BOLD),//Component.translatable("gui.showBlockScreen.workshop.copy"),
                        (btn) -> {
                            String res = ShareUtils.transfer(blockEntity.getTransformData());
                            setClipboard(res);
                            this.minecraft.getToasts().addToast(
                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.copy_pass"), Component.translatable("gui.showBlockScreen.workshop.share_hint"))
                            );
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3 + 40, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        //Component.translatable("gui.showBlockScreen.workshop.paste"),
        Button parseButton = Button.builder(Component.literal("\uD83D\uDCE5").withStyle(ChatFormatting.BOLD),//Component.translatable("gui.showBlockScreen.workshop.paste"),
                        (btn) -> {
                            String string = getClipboard();
                            try {
                                ShareUtils.ShareInformation shareInformation = ShareUtils.from(string);
                                checkModLack(shareInformation);
                                updateAllTransformData(shareInformation);
                                updateStateButtonVisible(true);
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
                                        String res = ShareUtils.transfer(blockEntity.getTransformData());
                                        try {
                                            EngraveItemResultLoader.save(res, string);
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

        blockStateList = new BlockStateIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        leftPropertyButton = Button.builder(Component.literal("<"),
                        (btn) -> property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, true))
                .bounds(RIGHT_COLUMN_X, RIGHT_STATE_PANEL_Y, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        rightPropertyButton = Button.builder(Component.literal(">"),
                        (btn) -> property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, false))
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2 * 3, RIGHT_STATE_PANEL_Y, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        leftStateButton = Button.builder(Component.literal("<"),
                        (btn) -> {
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE, (double) Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X, RIGHT_STATE_PANEL_Y + PER_HEIGHT, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        rightStateButton = Button.builder(Component.literal(">"),
                        (btn) -> {
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE, (double) Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2 * 3, RIGHT_STATE_PANEL_Y + PER_HEIGHT, SMALL_BUTTON_WIDTH, PER_HEIGHT)
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
                            case SLIDER -> Component.translatable("gui.showBlockScreen.mode.slider.tooltip");
                            case FINE_TUNE -> Component.translatable("gui.showBlockScreen.mode.fine_tune.tooltip");
                            case EDIT -> Component.translatable("gui.showBlockScreen.mode.edit.tooltip");
                        }
                ))
                .create(leftColumnX(), TOP, leftColumnWidth(), PER_HEIGHT, Component.literal("MODE"),
                        (btn, mode) -> {
                            switch (mode) {
                                case SLIDER -> panel.values().forEach(TransformComponent::setSliderStep);
                                case EDIT, FINE_TUNE -> panel.values().forEach(TransformComponent::setSliderFineTune);
                            }
                            switch (mode) {
                                case SLIDER, FINE_TUNE -> panel.values().forEach((it) -> it.triggerVisible(true));
                                case EDIT -> panel.values().forEach((it) -> it.triggerVisible(false));
                            }
                        }
                );

        choose(SCALE_X); // 首先放置scala_x, 因为pos_x依赖于它
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x), Math.abs(blockEntity.getTransformData(slot).pos.x));
        choose(POS_X).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posX,
                                () -> posX,
                                () -> getStep(posX),
                                (number) -> updateTransformData(POS_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(POS_X).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.X);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(0, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y), Math.abs(blockEntity.getTransformData(slot).pos.y));
        choose(POS_Y).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posY,
                                () -> posY,
                                () -> getStep(posY),
                                (number) -> updateTransformData(POS_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(POS_Y).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Y);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(1, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z), Math.abs(blockEntity.getTransformData(slot).pos.z));
        choose(POS_Z).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posZ,
                                () -> posZ,
                                () -> getStep(posZ),
                                (number) -> updateTransformData(POS_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
                        .step(choose(POS_Z).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Z);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(2, 0), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_X).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(ROT_X).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.X);
                            blockEntity.setShowRotAxis();
                        })
                        .initial(ROT_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(3, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Y).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(ROT_Y).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.setShowAxis(Direction.Axis.Y);
                            blockEntity.setShowRotAxis();
                        })
                        .initial(ROT_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(4, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Z).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformData(ROT_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
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
                        .text((caption, number) -> Component.translatable("gui.yuushya.showBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(choose(SCALE_X).setStandardStep(0.1))
                        .initial(SCALE_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(6, 20), leftColumnWidth(), PER_HEIGHT).build();

        choose(LIT).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.brightness_text"), 0.0, 15.0,
                                (number) -> updateTransformData(LIT, number))
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(LIT).setStandardStep(1))
                        .initial(LIT.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(7, 30), leftColumnWidth(), PER_HEIGHT).build();

        for (TransformComponent component : this.panel.values()) {
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
        this.addWidget(this.blockStateList);
        this.addRenderableWidget(addStateButton);
        this.addRenderableWidget(removeStateButton);
        this.addRenderableWidget(replaceButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(leftPropertyButton);
        this.addRenderableWidget(rightPropertyButton);
        this.addRenderableWidget(leftStateButton);
        this.addRenderableWidget(rightStateButton);
        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(parseButton);
        this.addRenderableWidget(saveButton);

        blockStateList.setSelectedSlot(slot);//updateStateButtonVisible();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.blockStateList.render(guiGraphics, mouseX, mouseY, partialTick);
        BlockState blockState = getBlockState();
        guiGraphics.drawString(this.font, this.blockStateList.updateRenderDisplayName(blockState), RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT, 0xFFFFFFFF, false);
        List<String> properties = this.blockStateList.updateRenderBlockStateProperties(blockState);
        for (int i = 0; i < properties.size(); i++) {
            MutableComponent displayBlockState = Component.literal(properties.get(i));
            guiGraphics.drawString(this.font, displayBlockState, RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT + this.font.lineHeight * (i + 1) + 1, 0xFFEBC6, false);
        }
        if (updateStateButtonVisible(false)) {
            guiGraphics.drawString(this.font, property.getName(), RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2, RIGHT_STATE_PANEL_Y + 5, 0xFFFFFFFF, false);
            guiGraphics.drawString(this.font, YuushyaDebugStickItem.getNameHelper(blockState, property), RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2, RIGHT_STATE_PANEL_Y + 5 + PER_HEIGHT, 0xFFFFFFFF, false);
        }
        if (modeButton.getValue() == Mode.EDIT) {
            for (TransformComponent component : this.panel.values()) {
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
        for (TransformType key : storage.keySet()) {
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(), slot, key, storage.get(key));
        }
        this.storage.clear();
        TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
    }

    public void checkModLack(ShareUtils.ShareInformation shareInformation) {
        List<String> unLoaded = shareInformation.mods().stream().filter(id -> !Platform.getModIds().contains(id)).toList();
        Minecraft.getInstance().getToasts().addToast(
                SystemToast.multiline(Minecraft.getInstance(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.literal("Mod Lack"), Component.literal(String.join(", ", unLoaded)))
        );
    }

    private void updateAllTransformData(ShareUtils.ShareInformation shareInformation) {
        List<TransformBlockData> dataList = blockEntity.getTransformData();
        BlockPos pos = blockEntity.getBlockPos();
        int currentSize = dataList.size();
        for (int slot = 0; slot < currentSize; slot++) {
            blockEntity.removeTransformData(slot);
            TransformDataNetwork.sendToServerSide(pos, slot, REMOVE, 0.0);
        }

        shareInformation.transfer(dataList);

        int nextSize = dataList.size();
        this.blockEntity.getLevel().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.storage.clear();
        for (int slot = 0; slot < nextSize; slot++) {
            TransformBlockData data = dataList.get(slot);
            updateTransformDataServerImmediate(data, slot);
        }
        TransformDataNetwork.sendToServerSideSuccess(pos);
        for (int slot = nextSize - 1; slot < currentSize; slot++) {
            blockEntity.setSlot(slot);
        }
        this.blockStateList.updateRenderList();
    }

    private void updateTransformDataServerImmediate(TransformBlockData data, int slot) {
        BlockPos pos = blockEntity.getBlockPos();
        TransformDataNetwork.sendToServerSide(pos, slot, POS_X, data.pos.x);
        TransformDataNetwork.sendToServerSide(pos, slot, POS_Y, data.pos.y);
        TransformDataNetwork.sendToServerSide(pos, slot, POS_Z, data.pos.z);

        TransformDataNetwork.sendToServerSide(pos, slot, ROT_X, data.rot.x);
        TransformDataNetwork.sendToServerSide(pos, slot, ROT_Y, data.rot.y);
        TransformDataNetwork.sendToServerSide(pos, slot, ROT_Z, data.rot.z);

        TransformDataNetwork.sendToServerSide(pos, slot, SCALE_X, data.scales.x);
        TransformDataNetwork.sendToServerSide(pos, slot, SCALE_Y, data.scales.y);
        TransformDataNetwork.sendToServerSide(pos, slot, SCALE_Z, data.scales.z);
        TransformDataNetwork.sendToServerSide(pos, slot, BLOCK_STATE, Block.getId(data.blockState));
        TransformDataNetwork.sendToServerSide(pos, slot, SHOWN, data.isShown ? 1 : 0);
    }

    private void updateTransformData(TransformType type, Double number) {
        this.storage.put(type, number);
        type.modify(blockEntity, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    private String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }

    private void setClipboard(String clipboardValue) {
        if (this.minecraft != null) {
            TextFieldHelper.setClipboardContents(this.minecraft, clipboardValue);
        }
    }

    public enum Mode implements StringRepresentable {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit");

        private final String name;
        @Getter
        private final Component symbol;

        Mode(String name) {
            this.name = name;
            this.symbol = Component.translatable("gui.showBlockScreen.mode." + name);
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

    }
}
