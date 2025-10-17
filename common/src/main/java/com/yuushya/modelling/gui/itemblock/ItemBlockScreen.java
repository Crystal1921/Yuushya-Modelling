package com.yuushya.modelling.gui.itemblock;

import com.yuushya.modelling.Yuushya;
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
import com.yuushya.modelling.gui.widget.SizeTransformComponent;
import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import com.yuushya.modelling.utils.YuushyaUtils;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.DISABLE_AO;
import static com.yuushya.modelling.blockentity.transformData.ItemTransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getStep;
import static com.yuushya.modelling.utils.YuushyaUtils.normalizeAngle;

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
    private final Map<ItemTransformType, SizeTransformComponent> panelSize = new LinkedHashMap<>();
    public EditBox colorEditBox;
    public Button colorFinishButton;
    public CycleButton<ApplyColor> colorApplyButton;
    private int slot;
    private ItemStack itemStack = ItemStack.EMPTY;
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private ItemStackIconList itemStackList;
    private ColorWidget colorWidget;

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

        for (SizeTransformComponent size : this.panelSize.values()) {
            size.setSliderInitial(this.blockEntity, this.slot);
        }

        double extract = COLOR.extract(blockEntity, slot);
        this.colorWidget.setColor((int) extract);
        this.colorEditBox.setValue(String.format("#%06X", (0xFFFFFF & (int) extract)));

        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
    }

    private ItemTransformComponent choose(ItemTransformType type) {
        return panel.computeIfAbsent(type, ItemTransformComponent::new);
    }

    private SizeTransformComponent chooseSize(ItemTransformType type) {
        return panelSize.computeIfAbsent(type, SizeTransformComponent::new);
    }

    private void updateColorVisible(boolean visible) {
        this.colorWidget.visible = visible;
        this.colorEditBox.visible = visible;
        this.colorFinishButton.visible = visible;
        this.colorApplyButton.visible = visible;
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
                            if (this.newItemStack != null) {
                                if (this.newItemStack.isEmpty()) return;
                                itemStackList.addSlot();
                                updateItemStack(newItemStack);
                                updateTransformData(SHOWN, 1.0);
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button copyItemButton = Button.builder(Component.literal("⧉"),
                        (btn) -> {
                            ItemStackIconList.Entry selected = this.itemStackList.getSelected();
                            if (selected != null) {
                                itemStackList.addSlot();
                                updateTransformData(selected.getTransformData());
                                itemStackList.setSelectedSlot(slot);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.copy")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button removeItemButton = Button.builder(Component.literal("×"),
                        (btn) -> updateTransformData(REMOVE, 0.0)
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

        shownStateButton = CycleButton.booleanBuilder(
                        Component.literal("🕶"),//Component.translatable("gui.itemBlockScreen.display.on"),
                        Component.literal("👀"))//Component.translatable("gui.itemBlockScreen.display.off"))
                .displayOnlyValue()
                .withInitialValue(true)
                .withTooltip((on) -> Tooltip.create(on ? Component.translatable("gui.showBlockScreen.display.on") : Component.translatable("gui.showBlockScreen.display.off")))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
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
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        Button parseButton = Button.builder(Component.literal("\uD83D\uDCE5").withStyle(ChatFormatting.BOLD),
                        (btn) -> {
                            String string = getClipboard();
                            try {
                                ShareUtils.ShareItemInformation shareInformation = ShareUtils.fromItems(string);
                                if (shareInformation.items().isEmpty()) {
                                    this.minecraft.getToasts().addToast(
                                            SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("gui.showBlockScreen.workshop.error"), Component.literal("No item data found")));
                                    return;
                                }
                                checkModLack(shareInformation);
                                updateAllTransformData(shareInformation);
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
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 7, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

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
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 8, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        CycleButton<Boolean> ambientOcclusionButton = CycleButton
                .booleanBuilder(Component.literal("●"), Component.literal("☀"))
                .displayOnlyValue()
                .withInitialValue(blockEntity.getBlockState().getValue(DISABLE_AO))
                .withTooltip((on -> Tooltip.create(on ? Component.translatable("gui.itemBlockScreen.ambientOcclusion.on") : Component.translatable("gui.itemBlockScreen.ambientOcclusion.off"))))
                .create(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 9, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT, Component.empty(),
                        (btn, enableAO) -> {
                            BlockState blockState = blockEntity.getBlockState();
                            Level level = blockEntity.getLevel();
                            if (level != null) {
                                level.setBlock(blockEntity.getBlockPos(), blockEntity.getBlockState().setValue(DISABLE_AO, enableAO), 18);
                                level.sendBlockUpdated(blockEntity.getBlockPos(), blockState, blockState, net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
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
                            case CUSTOM_SIZE -> Component.translatable("gui.itemBlockScreen.mode.custom_size.tooltip");
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
                                    panelSize.values().forEach((it) -> it.triggerVisible(false));
                                    updateColorVisible(false);
                                }
                                case EDIT -> {
                                    panel.values().forEach((it) -> it.triggerVisible(false));
                                    panelSize.values().forEach((it) -> it.triggerVisible(false));
                                    updateColorVisible(false);
                                }
                                case COLOR -> {
                                    panel.values().forEach(ItemTransformComponent::setInvisible);
                                    panelSize.values().forEach((it) -> it.triggerVisible(false));
                                    updateColorVisible(true);
                                }
                                case CUSTOM_SIZE -> {
                                    panel.values().forEach(ItemTransformComponent::setInvisible);
                                    panelSize.values().forEach((it) -> it.triggerVisible(true));
                                    updateColorVisible(false);
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

        chooseSize(SCALE_X).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(), 0.0, 1.0, 10.0,
                                (number) -> {
                                    updateTransformData(SCALE_X, number);
                                    chooseSize(SCALE_X).editBox.setValue(String.valueOf(number));
                                    choose(POS_X).sliderButton.setValidatedValue(choose(POS_X).sliderButton.getValidatedValue());
                                })
                        .text((caption, number) -> Component.translatable("gui.yuushya.itemBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(chooseSize(SCALE_X).setStandardStep(0.1))
                        .initial(SCALE_X.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(0, 20), leftColumnWidth(), PER_HEIGHT).build();

        chooseSize(SCALE_Y).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(), 0.0, 1.0, 10.0,
                                (number) -> {
                                    updateTransformData(SCALE_Y, number);
                                    chooseSize(SCALE_Y).editBox.setValue(String.valueOf(number));
                                    choose(POS_Y).sliderButton.setValidatedValue(choose(POS_Y).sliderButton.getValidatedValue());
                                })
                        .text((caption, number) -> Component.translatable("gui.yuushya.itemBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(chooseSize(SCALE_Y).setStandardStep(0.1))
                        .initial(SCALE_Y.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(2, 20), leftColumnWidth(), PER_HEIGHT).build();

        chooseSize(SCALE_Z).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(), 0.0, 1.0, 10.0,
                                (number) -> {
                                    updateTransformData(SCALE_Z, number);
                                    chooseSize(SCALE_Z).editBox.setValue(String.valueOf(number));
                                    choose(POS_Z).sliderButton.setValidatedValue(choose(POS_Z).sliderButton.getValidatedValue());
                                })
                        .text((caption, number) -> Component.translatable("gui.yuushya.itemBlockScreen.scale_text", String.format("%05.1f", number)))
                        .step(chooseSize(SCALE_Z).setStandardStep(0.1))
                        .initial(SCALE_Z.extract(blockEntity, slot))
                        .bounds(leftColumnX(), top(4, 20), leftColumnWidth(), PER_HEIGHT).build();

        this.colorWidget = new ColorWidget(leftColumnX() - 10, top(-2, 30), 110, 160, (int) COLOR.extract(blockEntity, slot), Component.translatable("gui.yuushya.itemBlockScreen.color_text"), this);
        this.colorEditBox = new EditBox(this.font, leftColumnX() - 5, top(6, 30), leftColumnWidth(), PER_HEIGHT, Component.translatable("gui.yuushya.itemBlockScreen.color_text"));
        this.colorEditBox.setMaxLength(7);
        this.colorFinishButton = Button.builder(Component.literal("√").withStyle(ChatFormatting.GREEN), (button -> {
            String text = colorEditBox.getValue();
            if (text.startsWith("#")) {
                try {
                    int color = Integer.parseInt(text.substring(1), 16);
                    colorWidget.setColor(color);
                    updateTransformData(COLOR, (double) color);
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
                                        updateTransformData(COLOR, (double) color, i);
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

        for (ItemTransformComponent component : this.panel.values()) {
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
        }

        for (SizeTransformComponent size : this.panelSize.values()) {
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
        this.addRenderableWidget(xMirror);
        this.addRenderableWidget(yMirror);
        this.addRenderableWidget(zMirror);
        this.addRenderableWidget(colorWidget);
        this.addRenderableWidget(colorEditBox);
        this.addRenderableWidget(colorFinishButton);
        this.addRenderableWidget(colorApplyButton);

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

        updateTransformData(POS_X, pos.x);
        updateTransformData(POS_Y, pos.y);
        updateTransformData(POS_Z, pos.z);

        Vector3f euler = rot.getEulerAnglesXYZ(new Vector3f());
        updateTransformData(ROT_X, normalizeAngle(Math.toDegrees(euler.x)));
        updateTransformData(ROT_Y, normalizeAngle(Math.toDegrees(euler.y)));
        updateTransformData(ROT_Z, normalizeAngle(Math.toDegrees(euler.z)));

        this.itemStackList.setSelectedSlot(slot);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.itemStackList.render(guiGraphics, mouseX, mouseY, partialTick);
        ItemStack itemStack = getItemStack();
        guiGraphics.drawString(this.font, itemStack.getDisplayName(), RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT, 0xFFFFFFFF, false);

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
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, COLOR, data.color);

        NetworkManager.sendToServer(new ItemStackPacket(pos, slot, data.itemStack));

    }

    private void updateTransformData(TransformItemData data) {
        updateTransformData(POS_X, data.pos.x);
        updateTransformData(POS_Y, data.pos.y);
        updateTransformData(POS_Z, data.pos.z);

        updateTransformData(ROT_X, (double) data.rot.x);
        updateTransformData(ROT_Y, (double) data.rot.y);
        updateTransformData(ROT_Z, (double) data.rot.z);

        updateTransformData(SCALE_X, (double) data.scales.x);
        updateTransformData(SCALE_Y, (double) data.scales.y);
        updateTransformData(SCALE_Z, (double) data.scales.z);

        updateTransformData(SHOWN, data.isShown ? 1.0 : 0.0);
        updateTransformData(COLOR, (double) data.color);

        updateItemStack(data.itemStack);
    }

    public void updateTransformData(ItemTransformType type, Double number) {
        this.storage.put(type, number);
        type.modify(blockEntity, slot, number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    public void updateTransformData(ItemTransformType type, Double number, int slot) {
        type.modify(blockEntity, slot, number);
        ItemTransformDataOncePacket.sendToServerSide(this.blockEntity.getBlockPos(), slot, type, number);
    }

    private void updateItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
        ITEM_STACK.modify(blockEntity, slot, itemStack);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
    }

    private String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }

    public static void setClipboard(String clipboardValue) {
        Minecraft mc = Minecraft.getInstance();
        TextFieldHelper.setClipboardContents(mc, clipboardValue);
    }

    public Font getFont() {
        return this.font;
    }

    public enum Mode implements StringRepresentable {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit"), COLOR("color"), CUSTOM_SIZE("custom_size");

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