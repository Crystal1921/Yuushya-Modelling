package com.yuushya.modelling.gui.textblock;

import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.AbstractTransformScreen;
import com.yuushya.modelling.gui.engrave.EngraveTextResultLoader;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.widget.*;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.TextLinesPacket;
import com.yuushya.modelling.network.TextTransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.REMOVE;
import static com.yuushya.modelling.blockentity.transformData.TextTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;

public class TextBlockScreen extends AbstractTransformScreen<TextTransformType, TextBlockEntity> {
    protected static final Button.CreateNarration DEFAULT_NARRATION = Supplier::get;
    private static final int[] rainbowColors = {
            Color.WHITE.getRGB(),
            Color.BLACK.getRGB(),
            Color.RED.getRGB(),
            Color.ORANGE.getRGB(),
            Color.YELLOW.getRGB(),
            Color.GREEN.getRGB(),
            Color.CYAN.getRGB(),
            Color.BLUE.getRGB(),
            Color.MAGENTA.getRGB()
    };
    private static final int RAINBOW_COUNT = rainbowColors.length;
    private final List<String> newTextLines;
    private final ColorButton[] rainbowColorButtons = new ColorButton[RAINBOW_COUNT];
    public CycleButton<Boolean> boldButton;
    public CycleButton<Boolean> italicButton;
    public CycleButton<Boolean> underlineButton;
    public CycleButton<Boolean> strikethroughButton;
    public CycleButton<Boolean> obfuscatedButton;
    public CycleButton<Boolean> fontButton;
    public ColorButton colorButton;
    public Button clearButton;
    public CycleButton<Boolean> cullButton;
    public CycleButton<Boolean> mirrorButton;
    public StyledMultiLineEditBox textEditBox;
    public FontList fontList;
    private List<String> textLines = new ArrayList<>();
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private TextIconList textIconList;

    public TextBlockScreen(TextBlockEntity blockEntity, List<String> newTextLines) {
        super(GameNarrator.NO_TITLE, blockEntity);
        this.newTextLines = newTextLines != null ? new ArrayList<>(newTextLines) : new ArrayList<>();
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    @Override
    protected double extract(TextTransformType type, int slot) {
        return type.extract(this.blockEntity, slot);
    }

    @Override
    protected void modify(TextTransformType type, int slot, double number) {
        type.modify(this.blockEntity, slot, number);
    }

    @Override
    protected void sendTransform(BlockPos pos, int slot, TextTransformType type, double number) {
        TextTransformDataOncePacket.sendToServerSide(pos, slot, type, number);
    }

    @Override
    protected void sendSuccess(BlockPos pos) {
        TextTransformDataOncePacket.sendToServerSideSuccess(pos);
    }

    @Override
    protected ShareController shareController() {
        return this.share;
    }

    private final ShareController share = new ShareController() {
        @Override
        public String serialize() {
            return ShareUtils.transferText(blockEntity.getTransformData());
        }

        @Override
        public void load(String clipboard) {
            ShareUtils.SharedTextInformation shareInformation = ShareUtils.fromText(clipboard);
            if (shareInformation.texts().isEmpty()) {
                throw new IllegalArgumentException("No item data found");
            }
            updateAllTransformData(shareInformation);
        }

        @Override
        public void save(String name) throws IOException {
            EngraveTextResultLoader.saveItem(ShareUtils.transferText(blockEntity.getTransformData()), name);
        }
    };

    @Override
    protected void flushSlotExtra() {
        flushTextLines();
    }

    @Override
    protected void flushOnRemoved() {
        flushTextLines();
    }

    private void flushTextLines() {
        if (!textLines.isEmpty()) {
            TextLinesPacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, textLines);
        }
        this.textLines = new ArrayList<>();
    }

    @Override
    protected void onSlotChanged() {
        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
        cullButton.setValue(this.blockEntity.getTransformData(slot).isCulled);
        mirrorButton.setValue(this.blockEntity.getTransformData(slot).isMirror);

        // Update text edit box with first line of current slot
        List<String> currentLines = this.blockEntity.getTransformData(slot).textLines;
        if (!currentLines.isEmpty()) {
            ClientLevel level = Minecraft.getInstance().level;
            List<Component> components = new ArrayList<>();
            for (String line : currentLines) {
                if (level != null) {
                    components.add(Component.Serializer.fromJson(line, level.registryAccess()));
                }
            }
            textEditBox.setValue(components);
        }
    }

    public List<String> getTextLines() {
        return blockEntity.getTransformData(slot).textLines;
    }

    @Override
    protected void init() {
        if (minecraft == null) return;
        Button addTextButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            textIconList.addSlot();
                            if (!newTextLines.isEmpty()) {
                                updateTextLines(new ArrayList<>(newTextLines));
                                updateTransformDataClient(SHOWN, 1.0);
                            } else {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) {
                                    return;
                                }
                                List<String> defaultText = new ArrayList<>();
                                List<Component> components = new ArrayList<>();
                                components.add(Component.literal("New Text"));
                                defaultText.add(Component.Serializer.toJson(Component.literal("New Text"), level.registryAccess()));
                                this.textEditBox.setValue(components);
                                updateTextLines(defaultText);
                                updateTransformDataClient(SHOWN, 1.0);
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button copyTextButton = Button.builder(Component.literal("⧉"),
                        (btn) -> {
                            TextIconList.Entry selected = this.textIconList.getSelected();
                            if (selected != null) {
                                textIconList.addSlot();
                                updateTransformDataClient(selected.getTransformData());
                                textIconList.setSelectedSlot(slot);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button removeTextButton = Button.builder(Component.literal("×"),
                        (btn) -> updateTransformDataClient(TextTransformType.REMOVE, 0.0)
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 2, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button copyButton = createCopyButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6);
        Button parseButton = createPasteButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 7);
        Button saveButton = createSaveButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 8);

        shownStateButton = createShownButton(SHOWN, RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3);

        cullButton = CycleButton.booleanBuilder(
                        Component.literal("▢"),
                        Component.literal("■"))
                .displayOnlyValue()
                .withInitialValue(CULLED.extract(blockEntity, slot) != 0)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.textBlockScreen.cull.on") : Component.translatable("gui.textBlockScreen.cull.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(CULLED, bl ? 1.0 : 0.0)
                );

        mirrorButton = CycleButton.booleanBuilder(
                        Component.literal("⇢"),
                        Component.literal("⇠"))
                .displayOnlyValue()
                .withInitialValue(MIRROR.extract(blockEntity, slot) != 0)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.textBlockScreen.mirror.on") : Component.translatable("gui.textBlockScreen.mirror.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 5, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(MIRROR, bl ? 1.0 : 0.0)
                );

        textIconList = new TextIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        CycleButton<BlockShape> shapeButton = createShapeButton(SHAPE, SHAPE.extractShape(blockEntity));

        modeButton = createModeButton(Mode.values(), Mode.SLIDER, (btn, mode) -> {
            switch (mode) {
                case SLIDER -> applySliderMode(false, true);
                case FINE_TUNE -> applySliderMode(true, true);
                case EDIT -> applySliderMode(true, false);
                case TEXT -> panel.values().forEach(TransformComponent::setInvisible);
            }
            setTextInputVisible(mode == Mode.TEXT);
        });

        // Setup transform components
        choose(SCALE_X);
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x()), Math.abs(blockEntity.getTransformData(slot).pos.x()));
        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y()), Math.abs(blockEntity.getTransformData(slot).pos.y()));
        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z()), Math.abs(blockEntity.getTransformData(slot).pos.z()));
        addTransformRows(List.of(POS_X, POS_Y, POS_Z), List.of(ROT_X, ROT_Y, ROT_Z),
                "gui.yuushya.showBlockScreen.", "block.yuushya.showblock.", posX, posY, posZ);

        choose(SCALE_X).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(), 0.0, 1.0, 10.0,
                                (number) -> {
                                    if (number == 0.0) number = 1.0;
                                    updateTransformDataClient(SCALE_X, number);
                                    updateTransformDataClient(SCALE_Y, number);
                                    updateTransformDataClient(SCALE_Z, number);
                                    choose(SCALE_X).sliderButton.setValidatedValue(number);
                                    choose(POS_X).sliderButton.setValidatedValue(choose(POS_X).sliderButton.getValidatedValue());
                                    choose(POS_Y).sliderButton.setValidatedValue(choose(POS_Y).sliderButton.getValidatedValue());
                                    choose(POS_Z).sliderButton.setValidatedValue(choose(POS_Z).sliderButton.getValidatedValue());
                                })
                        .text((caption, number) -> Component.translatable("gui.yuushya.showBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(choose(SCALE_X).setStandardStep(0.1))
                        .initial(SCALE_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(6, 20), leftColumnWidth(), PER_HEIGHT).build();

        addBrightnessRow(LIT, Component.translatable("gui.yuushya.showBlockScreen.brightness_text"), 7);

        this.colorEditBox = new EditBox(this.font, leftColumnX() - 5, top(6, 30), leftColumnWidth(), PER_HEIGHT, Component.translatable("gui.yuushya.itemBlockScreen.color_text"));
        this.colorEditBox.setMaxLength(7);

        fontList = new FontList(this.minecraft, this, 200, 160, leftColumnX() - 200, top(0, 30), 30);

        boldButton = CycleButton.booleanBuilder(Component.literal("B").withStyle(Style.EMPTY.withBold(true).withColor(Color.RED.getRGB())), Component.literal("B").withStyle(Style.EMPTY.withBold(true)))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() - RIGHT_BAR_WIDTH, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        italicButton = CycleButton.booleanBuilder(Component.literal("I").withStyle(Style.EMPTY.withItalic(true).withColor(Color.RED.getRGB())), Component.literal("I").withStyle(Style.EMPTY.withItalic(true)))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX(), top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        underlineButton = CycleButton.booleanBuilder(Component.literal("U").withStyle(Style.EMPTY.withUnderlined(true).withColor(Color.RED.getRGB())), Component.literal("U").withStyle(Style.EMPTY.withUnderlined(true)))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        strikethroughButton = CycleButton.booleanBuilder(Component.literal("S").withStyle(Style.EMPTY.withStrikethrough(true).withColor(Color.RED.getRGB())), Component.literal("S").withStyle(Style.EMPTY.withStrikethrough(true)))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH * 2, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        obfuscatedButton = CycleButton.booleanBuilder(Component.literal("O").withStyle(Style.EMPTY.withObfuscated(true).withColor(Color.RED.getRGB())), Component.literal("O").withStyle(Style.EMPTY.withObfuscated(true)))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH * 3, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        fontButton = CycleButton.booleanBuilder(Component.translatable("gui.textBlockScreen.font"), Component.translatable("gui.textBlockScreen.font"))
                .displayOnlyValue()
                .withInitialValue(Boolean.FALSE)
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() - RIGHT_BAR_WIDTH * 2, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.fontList.visible = bl;
                            if (this.colorWidget.visible) {
                                this.colorWidget.visible = !bl;
                                colorButton.showEditor = !bl;
                                for (ColorButton rainbowColorButton : this.rainbowColorButtons) {
                                    rainbowColorButton.visible = !bl;
                                }
                            }
                            Map<ResourceLocation, FontSet> fontSets = Minecraft.getInstance().fontManager.fontSets;
                            this.fontList.updateRenderList(new ArrayList<>(fontSets.keySet()));
                        });

        clearButton = Button.builder(Component.literal("C"), (button) -> {
            this.textEditBox.clearStyle();
        }).bounds(leftColumnX() + RIGHT_BAR_WIDTH * 4, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        colorButton = new ColorButton(leftColumnX() - RIGHT_BAR_WIDTH * 3, top(0, 0), PER_HEIGHT, PER_HEIGHT, (button) -> {
            colorButton.showEditor = !colorButton.showEditor;
            colorWidget.visible = colorButton.showEditor;
            if (fontList.visible) {
                fontList.visible = !colorButton.showEditor;
                fontButton.setValue(!colorButton.showEditor);
            }
            for (ColorButton rainbowColorButton : this.rainbowColorButtons) {
                rainbowColorButton.visible = colorButton.showEditor;
            }
        }, DEFAULT_NARRATION);

        this.colorWidget = new ColorWidget(leftColumnX() - 120, top(0, 30), 110, 160, Color.WHITE.getRGB(), Component.translatable("gui.yuushya.itemBlockScreen.color_text"), this);
        for (int i = 0; i < rainbowColors.length; i++) {
            rainbowColorButtons[i] = new ColorButton(leftColumnX() - RIGHT_BAR_WIDTH * (4 + i), top(0, 0), PER_HEIGHT, PER_HEIGHT, (button) -> {
                if (button instanceof ColorButton selectButton) {
                    this.colorWidget.setColor(selectButton.color);
                    colorButton.color = selectButton.color;
                    this.textEditBox.setColorTextStyle();
                }
            }, DEFAULT_NARRATION, rainbowColors[i]);
            rainbowColorButtons[i].visible = false;
        }

        boldButton.visible = false;
        italicButton.visible = false;
        underlineButton.visible = false;
        strikethroughButton.visible = false;
        obfuscatedButton.visible = false;
        clearButton.visible = false;
        fontButton.visible = false;
        colorButton.visible = false;
        colorEditBox.visible = false;
        colorWidget.visible = false;
        fontList.visible = false;


        // Text editing section
        textEditBox = new StyledMultiLineEditBox(this.font, leftColumnX(), top(1, 0), leftColumnWidth(), PER_HEIGHT * 8, Component.literal(""), Component.literal("Text"), this);
        textEditBox.visible = false;

        List<String> currentLines = this.blockEntity.getTransformData(slot).textLines;
        if (!currentLines.isEmpty()) {
            ClientLevel level = Minecraft.getInstance().level;
            List<Component> components = new ArrayList<>();
            for (String line : currentLines) {
                if (level != null) {
                    components.add(Component.Serializer.fromJson(line, level.registryAccess()));
                }
            }
            textEditBox.setValue(components);
            TextColor textColor = components.getFirst().getStyle().getColor();
            if (textColor != null) {
                colorWidget.setColor(textColor.getValue());
            }
        }

        choose(SCALE_X).decoration(Component.translatable("gui.yuushya.showBlockScreen.scale_text", ""));

        registerPanelWidgets();

        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(parseButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(modeButton);
        this.addRenderableWidget(shapeButton);
        this.addRenderableWidget(cullButton);
        this.addRenderableWidget(mirrorButton);
        this.addWidget(this.textIconList);
        this.addRenderableWidget(addTextButton);
        this.addRenderableWidget(removeTextButton);
        this.addRenderableWidget(copyTextButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(textEditBox);
        this.addRenderableWidget(boldButton);
        this.addRenderableWidget(italicButton);
        this.addRenderableWidget(underlineButton);
        this.addRenderableWidget(strikethroughButton);
        this.addRenderableWidget(obfuscatedButton);
        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(colorButton);
        this.addRenderableWidget(fontButton);
        this.addRenderableWidget(colorWidget);
        this.addRenderableWidget(fontList);

        for (ColorButton rainbowColorButton : this.rainbowColorButtons) {
            this.addRenderableWidget(rainbowColorButton);
        }

        textIconList.setSelectedSlot(slot);
    }

    @Override
    protected boolean isEditMode() {
        return modeButton.getValue() == Mode.EDIT;
    }

    @Override
    protected void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.textIconList.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateTransformDataClient(TransformTextData data) {
        updateTransformDataClient(POS_X, data.pos.x);
        updateTransformDataClient(POS_Y, data.pos.y);
        updateTransformDataClient(POS_Z, data.pos.z);

        updateTransformDataClient(ROT_X, (double) data.rot.x);
        updateTransformDataClient(ROT_Y, (double) data.rot.y);
        updateTransformDataClient(ROT_Z, (double) data.rot.z);

        updateTransformDataClient(SCALE_X, (double) data.scales.x);
        updateTransformDataClient(SCALE_Y, (double) data.scales.y);
        updateTransformDataClient(SCALE_Z, (double) data.scales.z);

        updateTransformDataClient(CULLED, data.isCulled ? 1.0 : 0.0);
        updateTransformDataClient(MIRROR, data.isMirror ? 1.0 : 0.0);
        updateTransformDataClient(SHOWN, data.isShown ? 1.0 : 0.0);

        updateTextLines(data.textLines);
    }

    private void updateAllTransformData(ShareUtils.SharedTextInformation shareInformation) {
        List<TransformTextData> dataList = blockEntity.getTransformData();
        BlockPos pos = blockEntity.getBlockPos();
        int currentSize = dataList.size();
        for (int slot = 0; slot < currentSize; slot++) {
            blockEntity.removeTransformData(slot);
            ItemTransformDataOncePacket.sendToServerSide(pos, slot, REMOVE, 0.0);
        }

        shareInformation.transferTexts(dataList);

        int nextSize = dataList.size();
        this.blockEntity.getLevel().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
        this.storage.clear();
        for (int slot = 0; slot < nextSize; slot++) {
            TransformTextData data = dataList.get(slot);
            updateTransformDataServerImmediate(data, slot);
        }
        ItemTransformDataOncePacket.sendToServerSideSuccess(pos);
        for (int slot = nextSize - 1; slot < currentSize; slot++) {
            blockEntity.setSlot(slot);
        }
        this.textIconList.updateRenderList();
    }

    private void updateTransformDataServerImmediate(TransformTextData data, int slot) {
        if (data.textLines.isEmpty()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        TextTransformDataOncePacket.sendToServerSide(pos, slot, POS_X, data.pos.x);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, POS_Y, data.pos.y);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, POS_Z, data.pos.z);

        TextTransformDataOncePacket.sendToServerSide(pos, slot, ROT_X, data.rot.x);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Y, data.rot.y);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, ROT_Z, data.rot.z);

        TextTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_X, data.scales.x);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Y, data.scales.y);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Z, data.scales.z);

        TextTransformDataOncePacket.sendToServerSide(pos, slot, SHOWN, data.isShown ? 1 : 0);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, CULLED, data.isCulled ? 1 : 0);
        TextTransformDataOncePacket.sendToServerSide(pos, slot, MIRROR, data.isMirror ? 1 : 0);

        TextLinesPacket.sendToServerSide(pos, slot, data.textLines);
    }

    private void updateTextLines(List<String> textLines) {
        this.textLines = new ArrayList<>(textLines);
        TEXT_LINES.modify(blockEntity, slot, textLines);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    public void updateComponentLines(List<Component> textLines) {
        List<String> lines = new ArrayList<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (Component line : textLines) {
            lines.add(Component.Serializer.toJson(line, level.registryAccess()));
        }
        updateTextLines(lines);
    }

    private void setTextInputVisible(boolean visible) {
        this.textEditBox.visible = visible;
        this.boldButton.visible = visible;
        this.italicButton.visible = visible;
        this.underlineButton.visible = visible;
        this.strikethroughButton.visible = visible;
        this.obfuscatedButton.visible = visible;
        this.clearButton.visible = visible;
        this.fontButton.visible = visible;
        this.colorEditBox.visible = visible;
        this.colorButton.visible = visible;
        this.colorWidget.visible = false;
        this.colorButton.showEditor = false;
        for (ColorButton rainbowColorButton : this.rainbowColorButtons) {
            rainbowColorButton.visible = false;
        }
    }

    @Override
    public void updateColorData(int colorValue) {
        this.colorButton.color = colorValue;
        this.textEditBox.setColorTextStyle();
    }

    public enum Mode implements ScreenMode {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit"), TEXT("text");

        private final String name;
        @Getter
        private final Component symbol;

        Mode(String name) {
            this.name = name;
            this.symbol = Component.translatable("gui.showBlockScreen.mode." + name);
        }

        @Override
        public Component getTooltip() {
            return Component.translatable("gui.showBlockScreen.mode." + name + ".tooltip");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
