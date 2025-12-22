package com.yuushya.modelling.gui.textblock;

import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.gui.widget.TextIconList;
import com.yuushya.modelling.gui.widget.TextTransformComponent;
import com.yuushya.modelling.network.TextLinesPacket;
import com.yuushya.modelling.network.TextTransformDataOncePacket;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.modelling.blockentity.transformData.TextTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;

public class TextBlockScreen extends Screen {
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

    private final TextBlockEntity blockEntity;
    private final List<String> newTextLines;
    private final Map<TextTransformType, Double> storage = new HashMap<>();
    private final Map<TextTransformType, TextTransformComponent> panel = new LinkedHashMap<>();
    private int slot;
    private List<String> textLines = new ArrayList<>();
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private TextIconList textIconList;
    private EditBox textEditBox;
    private Button addLineButton;
    private Button removeLineButton;

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
        
        // Update text edit box with first line of current slot
        List<String> currentLines = this.blockEntity.getTransformData(slot).textLines;
        if (!currentLines.isEmpty()) {
            this.textEditBox.setValue(currentLines.get(0));
        } else {
            this.textEditBox.setValue("");
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
        if (minecraft == null) return;

        Button addTextButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            textIconList.addSlot();
                            if (!newTextLines.isEmpty()) {
                                updateTextLines(new ArrayList<>(newTextLines));
                                updateTransformDataClient(SHOWN, 1.0);
                            } else {
                                List<String> defaultText = new ArrayList<>();
                                defaultText.add("New Text");
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
                        (btn) -> updateTransformDataClient(REMOVE, 0.0)
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 2, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = CycleButton.booleanBuilder(
                        Component.literal("🕶"),
                        Component.literal("👀"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, bl) -> updateTransformDataClient(SHOWN, bl ? 1.0 : 0.0)
                );

        textIconList = new TextIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        // Text editing section
        textEditBox = new EditBox(this.font, RIGHT_COLUMN_X, RIGHT_STATE_PANEL_Y, RIGHT_LIST_WIDTH + 100, PER_HEIGHT, Component.literal("Text"));
        textEditBox.setMaxLength(256);
        List<String> currentLines = this.blockEntity.getTransformData(slot).textLines;
        if (!currentLines.isEmpty()) {
            textEditBox.setValue(currentLines.get(0));
        }

        addLineButton = Button.builder(Component.literal("+"),
                        (btn) -> {
                            String newLine = textEditBox.getValue();
                            if (!newLine.isEmpty()) {
                                List<String> lines = new ArrayList<>(blockEntity.getTransformData(slot).textLines);
                                lines.add(newLine);
                                updateTextLines(lines);
                                textEditBox.setValue("");
                            }
                        })
                .tooltip(Tooltip.create(Component.literal("Add text line")))
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH + 105, RIGHT_STATE_PANEL_Y, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        removeLineButton = Button.builder(Component.literal("-"),
                        (btn) -> {
                            List<String> lines = new ArrayList<>(blockEntity.getTransformData(slot).textLines);
                            if (!lines.isEmpty()) {
                                lines.remove(lines.size() - 1);
                                updateTextLines(lines);
                            }
                        })
                .tooltip(Tooltip.create(Component.literal("Remove last text line")))
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH + 105 + RIGHT_BAR_WIDTH, RIGHT_STATE_PANEL_Y, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        CycleButton<BlockShape> shapeButton = CycleButton.builder(BlockShape::getSymbol)
                .displayOnlyValue()
                .withValues(BlockShape.values())
                .withInitialValue(SHAPE.extractShape(blockEntity))
                .create(leftColumnX() - 50, TOP, 40, PER_HEIGHT, Component.literal("shape"),
                        (button, shape) -> updateTransformDataClient(SHAPE, (double) shape.ordinal()));

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
                                case SLIDER -> panel.values().forEach(TextTransformComponent::setSliderStep);
                                case EDIT, FINE_TUNE -> panel.values().forEach(TextTransformComponent::setSliderFineTune);
                            }
                            switch (mode) {
                                case SLIDER, FINE_TUNE -> panel.values().forEach((it) -> it.triggerVisible(true));
                                case EDIT -> panel.values().forEach((it) -> it.triggerVisible(false));
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
                            blockEntity.setShowAxis(Direction.Axis.X);
                            blockEntity.setShowPosAxis();
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
                            blockEntity.setShowAxis(Direction.Axis.Y);
                            blockEntity.setShowPosAxis();
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
                            blockEntity.setShowAxis(Direction.Axis.Z);
                            blockEntity.setShowPosAxis();
                        })
                        .initial(POS_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(2, 0), leftColumnWidth(), PER_HEIGHT).build();

        choose(ROT_X).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("gui.yuushya.showBlockScreen.rot_text"), 0.0, 360.0,
                                (number) -> updateTransformDataClient(ROT_X, number))
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
                                (number) -> updateTransformDataClient(ROT_Y, number))
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
                                (number) -> updateTransformDataClient(ROT_Z, number))
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
                                    updateTransformDataClient(SCALE_X, number);
                                    updateTransformDataClient(SCALE_Y, number);
                                    updateTransformDataClient(SCALE_Z, number);
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

        for (TextTransformComponent component : this.panel.values()) {
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
        this.addWidget(this.textIconList);
        this.addRenderableWidget(addTextButton);
        this.addRenderableWidget(removeTextButton);
        this.addRenderableWidget(copyTextButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(textEditBox);
        this.addRenderableWidget(addLineButton);
        this.addRenderableWidget(removeLineButton);

        textIconList.setSelectedSlot(slot);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.textIconList.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render text lines info
        List<String> lines = getTextLines();
        guiGraphics.drawString(this.font, Component.literal("Lines: " + lines.size()), RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT, 0xFFFFFFFF, false);
        
        // Render first few lines as preview
        int previewY = TOP + 6 + PER_HEIGHT + this.font.lineHeight + 2;
        for (int i = 0; i < Math.min(lines.size(), 5); i++) {
            String line = lines.get(i);
            if (line.length() > 20) {
                line = line.substring(0, 20) + "...";
            }
            guiGraphics.drawString(this.font, line, RIGHT_STATE_INFORM_X, previewY + i * (this.font.lineHeight + 1), 0xFFEBC6, false);
        }
        if (lines.size() > 5) {
            guiGraphics.drawString(this.font, "... (" + (lines.size() - 5) + " more)", RIGHT_STATE_INFORM_X, previewY + 5 * (this.font.lineHeight + 1), 0xAAAAAA, false);
        }

        if (modeButton.getValue() == Mode.EDIT) {
            for (TextTransformComponent component : this.panel.values()) {
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

        updateTransformDataClient(SHOWN, data.isShown ? 1.0 : 0.0);

        updateTextLines(data.textLines);
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
