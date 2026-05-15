package com.yuushya.modelling.gui.textblock;

import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.engrave.EngraveTextResultLoader;
import com.yuushya.modelling.gui.showblock.EditScreen;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.widget.*;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.TextLinesPacket;
import com.yuushya.modelling.network.TextTransformDataOncePacket;
import com.yuushya.modelling.utils.DeprecatedMethod;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.REMOVE;
import static com.yuushya.modelling.blockentity.transformData.TextTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;
import static com.yuushya.modelling.utils.ClientMethod.getClipboard;
import static com.yuushya.modelling.utils.ClientMethod.setClipboard;

public class TextBlockScreen extends AbstractColorScreen {
    public static final int PER_HEIGHT = 20;
    public static final int SMALL_BUTTON_WIDTH = 10;
    protected static final Button.CreateNarration DEFAULT_NARRATION = Supplier::get;
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
    private final TextBlockEntity blockEntity;
    private final List<String> newTextLines;
    private final Map<TextTransformType, Double> storage = new HashMap<>();
    private final Map<TextTransformType, TextTransformComponent> panel = new LinkedHashMap<>();
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
    public EditBox colorEditBox;
    public ColorWidget colorWidget;
    public StyledMultiLineEditBox textEditBox;
    public FontList fontList;
    private int slot;
    private List<String> textLines = new ArrayList<>();
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private TextIconList textIconList;

    public TextBlockScreen(TextBlockEntity blockEntity, List<String> newTextLines) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
        this.newTextLines = newTextLines != null ? new ArrayList<>(newTextLines) : new ArrayList<>();
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    // i \in [1,...]
    private static int top(int i, int offset) {
        return TOP + PER_HEIGHT + 10 + PER_HEIGHT * i + offset;
    }

    public void setSlot(int slot) {
        for (TextTransformType key : this.storage.keySet()) {
            TextTransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, key, this.storage.get(key));
        }

        if (!textLines.isEmpty()) {
            TextLinesPacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, textLines);
        }

        this.textLines = new ArrayList<>();
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for (TextTransformComponent component : this.panel.values()) {
            component.setSliderInitial(this.blockEntity, this.slot);
        }

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
                    components.add(DeprecatedMethod.fromJson(line, level.registryAccess()));
                }
            }
            textEditBox.setValue(components);
        }
    }

    private TextTransformComponent choose(TextTransformType type) {
        return panel.computeIfAbsent(type, TextTransformComponent::new);
    }

    public List<String> getTextLines() {
        return blockEntity.getTransformData(slot).textLines;
    }

    private int leftColumnX() {
        return this.width / 4 * 3 + 10;
    }

    private int leftColumnWidth() {
        return this.width / 4 - 20;
    }

    @Override
    protected void init() {
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
                                defaultText.add(DeprecatedMethod.toJson(Component.literal("New Text"), level.registryAccess()));
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

        Button copyButton = Button.builder(Component.literal("\uD83D\uDCE4").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            String res = ShareUtils.transferText(blockEntity.getTransformData());
                            setClipboard(res);
                            this.minecraft.getToastManager().addToast(
                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.copy_pass"), Component.translatable("gui.showBlockScreen.workshop.share_hint"))
                            );
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button parseButton = Button.builder(Component.literal("\uD83D\uDCE5").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            String string = getClipboard();
                            try {
                                ShareUtils.SharedTextInformation shareInformation = ShareUtils.fromText(string);
                                if (shareInformation.texts().isEmpty()) {
                                    this.minecraft.getToastManager().addToast(
                                            SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop.error"), Component.literal("No item data found")));
                                    return;
                                }
                                updateAllTransformData(shareInformation);
                                this.minecraft.getToastManager().addToast(
                                        new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.paste_pass"), null)
                                );
                            } catch (Exception e) {
                                this.minecraft.getToastManager().addToast(
                                        SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop.error"), Component.literal(e.getMessage()))
                                );
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.workshop.paste")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 7, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button saveButton = Button.builder(Component.literal("\uD83D\uDCBE").withStyle(ChatFormatting.BOLD),
                        (btn) -> this.minecraft.setScreen(new EditScreen(this,
                                Component.translatable("gui.showBlockScreen.workshop.save"),
                                Component.translatable("gui.showBlockScreen.workshop.save.tip"),
                                (string) -> {
                                    if (string != null) {
                                        String res = ShareUtils.transferText(blockEntity.getTransformData());
                                        try {
                                            EngraveTextResultLoader.saveItem(res, string);
                                            this.minecraft.getToastManager().addToast(
                                                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("gui.showBlockScreen.workshop.save_pass"), Component.translatable("gui.showBlockScreen.workshop.share_hint"))
                                            );
                                        } catch (IOException e) {
                                            this.minecraft.getToastManager().addToast(
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
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 8, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = CycleButton.booleanBuilder(
                        Component.literal("🕶"),
                        Component.literal("👀"), true)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(SHOWN, bl ? 1.0 : 0.0)
                );

        cullButton = CycleButton.booleanBuilder(
                        Component.literal("▢"),
                        Component.literal("■"), CULLED.extract(blockEntity, slot) != 0)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.textBlockScreen.cull.on") : Component.translatable("gui.textBlockScreen.cull.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(CULLED, bl ? 1.0 : 0.0)
                );

        mirrorButton = CycleButton.booleanBuilder(
                        Component.literal("⇢"),
                        Component.literal("⇠"), MIRROR.extract(blockEntity, slot) != 0)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.textBlockScreen.mirror.on") : Component.translatable("gui.textBlockScreen.mirror.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 5, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(MIRROR, bl ? 1.0 : 0.0)
                );

        textIconList = new TextIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        CycleButton<BlockShape> shapeButton = CycleButton.builder(BlockShape::getSymbol, SHAPE.extractShape(blockEntity))
                .displayOnlyValue()
                .withValues(BlockShape.values())
                .create(leftColumnX() - 50, TOP, 40, PER_HEIGHT, Component.literal("shape"),
                        (button, shape) -> updateTransformDataClient(SHAPE, (double) shape.ordinal()));

        modeButton = CycleButton.builder(Mode::getSymbol, Mode.SLIDER)
                .displayOnlyValue()
                .withValues(Mode.values())
                .withTooltip((mode) -> Tooltip.create(
                        switch (mode) {
                            case Mode.SLIDER -> Component.translatable("gui.showBlockScreen.mode.slider.tooltip");
                            case Mode.FINE_TUNE -> Component.translatable("gui.showBlockScreen.mode.fine_tune.tooltip");
                            case Mode.EDIT -> Component.translatable("gui.showBlockScreen.mode.edit.tooltip");
                            case Mode.TEXT -> Component.translatable("gui.showBlockScreen.mode.text.tooltip");
                        }
                ))
                .create(leftColumnX(), TOP, leftColumnWidth(), PER_HEIGHT, Component.literal("MODE"),
                        (btn, mode) -> {
                            var values = panel.values();
                            switch (mode) {
                                case Mode.SLIDER -> values.forEach(it -> {
                                    it.setSliderStep();
                                    it.triggerVisible(true);
                                    setTextInputVisible(false);
                                });
                                case Mode.EDIT -> values.forEach(it -> {
                                    it.setSliderFineTune();
                                    it.triggerVisible(false);
                                    setTextInputVisible(false);
                                });
                                case Mode.FINE_TUNE -> values.forEach(it -> {
                                    it.setSliderFineTune();
                                    it.triggerVisible(true);
                                    setTextInputVisible(false);
                                });

                                case Mode.TEXT -> values.forEach(textTransformComponent -> {
                                    textTransformComponent.setInvisible();
                                    setTextInputVisible(true);
                                });
                            }
                        }
                );

        // Setup transform components
        choose(SCALE_X);
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x()), Math.abs(blockEntity.getTransformData(slot).pos.x()));
        choose(POS_X).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posX,
                                () -> posX,
                                () -> getStep(posX),
                                (number) -> updateTransformDataClient(POS_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(POS_X).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.X);
                        })
                        .initial(POS_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(0, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y()), Math.abs(blockEntity.getTransformData(slot).pos.y()));
        choose(POS_Y).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posY,
                                () -> posY,
                                () -> getStep(posY),
                                (number) -> updateTransformDataClient(POS_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(POS_Y).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.Y);
                        })
                        .initial(POS_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(1, 0), leftColumnWidth(), PER_HEIGHT).build();

        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z()), Math.abs(blockEntity.getTransformData(slot).pos.z()));
        choose(POS_Z).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.pos_text"),
                                () -> -posZ,
                                () -> posZ,
                                () -> getStep(posZ),
                                (number) -> updateTransformDataClient(POS_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
                        .step(choose(POS_Z).setStandardStep(0.0))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.Z);
                        })
                        .initial(POS_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(2, 0), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_X).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformDataClient(ROT_X, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", number)).withStyle(ChatFormatting.DARK_RED)))
                        .step(choose(ROT_X).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.X);
                        })
                        .initial(ROT_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(3, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Y).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformDataClient(ROT_Y, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", number)).withStyle(ChatFormatting.GREEN)))
                        .step(choose(ROT_Y).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.Y);
                        })
                        .initial(ROT_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(4, 10), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_Z).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformDataClient(ROT_Z, number))
                        .text((caption, number) -> Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", number)).withStyle(ChatFormatting.BLUE)))
                        .step(choose(ROT_Z).setStandardStep(22.5))
                        .onMouseOver((btn) -> {
                            blockEntity.highlightAxis(Direction.Axis.Z);
                        })
                        .initial(ROT_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(5, 10), leftColumnWidth(), PER_HEIGHT).build();

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

        choose(LIT).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.brightness_text"), 0.0, 15.0,
                                (number) -> updateTransformDataClient(LIT, number))
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(LIT).setStandardStep(1))
                        .initial(LIT.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(7, 30), leftColumnWidth(), PER_HEIGHT).build();

        this.colorEditBox = new EditBox(this.font, leftColumnX() - 5, top(6, 30), leftColumnWidth(), PER_HEIGHT, Component.translatable("gui.yuushya.itemBlockScreen.color_text"));
        this.colorEditBox.setMaxLength(7);

        fontList = new FontList(this.minecraft, this, 200, 160, leftColumnX() - 200, top(0, 30), 30);

        boldButton = CycleButton.booleanBuilder(Component.literal("B").withStyle(Style.EMPTY.withBold(true).withColor(Color.RED.getRGB())), Component.literal("B").withStyle(Style.EMPTY.withBold(true)), Boolean.FALSE)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() - RIGHT_BAR_WIDTH, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        italicButton = CycleButton.booleanBuilder(Component.literal("I").withStyle(Style.EMPTY.withItalic(true).withColor(Color.RED.getRGB())), Component.literal("I").withStyle(Style.EMPTY.withItalic(true)), Boolean.FALSE)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX(), top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        underlineButton = CycleButton.booleanBuilder(Component.literal("U").withStyle(Style.EMPTY.withUnderlined(true).withColor(Color.RED.getRGB())), Component.literal("U").withStyle(Style.EMPTY.withUnderlined(true)), Boolean.FALSE)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        strikethroughButton = CycleButton.booleanBuilder(Component.literal("S").withStyle(Style.EMPTY.withStrikethrough(true).withColor(Color.RED.getRGB())), Component.literal("S").withStyle(Style.EMPTY.withStrikethrough(true)), Boolean.FALSE)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH * 2, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        obfuscatedButton = CycleButton.booleanBuilder(Component.literal("O").withStyle(Style.EMPTY.withObfuscated(true).withColor(Color.RED.getRGB())), Component.literal("O").withStyle(Style.EMPTY.withObfuscated(true)), Boolean.FALSE)
                .displayOnlyValue()
                .withTooltip((on) -> Tooltip.create(Component.empty()))
                .create(leftColumnX() + RIGHT_BAR_WIDTH * 3, top(0, 0), RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> {
                            this.textEditBox.setTextStyle();
                        });

        fontButton = CycleButton.booleanBuilder(Component.translatable("gui.textBlockScreen.font"), Component.translatable("gui.textBlockScreen.font"), Boolean.FALSE)
                .displayOnlyValue()
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
                            Map<Identifier, FontSet> fontSets = Minecraft.getInstance().fontManager.fontSets;
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
                    components.add(DeprecatedMethod.fromJson(line, level.registryAccess()));
                }
            }
            textEditBox.setValue(components);
            TextColor textColor = components.getFirst().getStyle().getColor();
            if (textColor != null) {
                colorWidget.setColor(textColor.getValue());
            }
        }

        for (TextTransformComponent component : this.panel.values()) {
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
        }

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
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.textIconList.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (modeButton.getValue() == Mode.EDIT) {
            for (TextTransformComponent component : this.panel.values()) {
                guiGraphics.text(this.font, component.editBox.getMessage(), component.editBox.getX() + component.editBox.getWidth() / 2, component.editBox.getY() + component.editBox.getHeight() / 3, 0x707070);
                component.editBox.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        for (TextTransformType key : storage.keySet()) {
            TextTransformDataOncePacket.sendToServerSide(blockEntity.getBlockPos(), slot, key, storage.get(key));
        }
        if (!textLines.isEmpty()) {
            TextLinesPacket.sendToServerSide(this.blockEntity.getBlockPos(), this.slot, textLines);
        }

        this.textLines = new ArrayList<>();
        this.storage.clear();
        TextTransformDataOncePacket.sendToServerSideSuccess(blockEntity.getBlockPos());
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

    public void updateTransformDataClient(TextTransformType type, Double number) {
        this.storage.put(type, number);
        type.modify(blockEntity, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
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
            lines.add(DeprecatedMethod.toJson(line, level.registryAccess()));
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
    public Font getColorFont() {
        return this.font;
    }

    @Override
    public EditBox getColorEditBox() {
        return colorEditBox;
    }

    @Override
    public void updateColorData(int colorValue) {
        this.colorButton.color = colorValue;
        this.textEditBox.setColorTextStyle();
    }

    @Override
    public void setColorFocused(AbstractWidget widget) {
        this.setFocused(widget);
    }

    @Override
    public void setColorDragging(boolean dragging) {
        this.setDragging(dragging);
    }

    public enum Mode implements StringRepresentable {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit"), TEXT("text");

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
