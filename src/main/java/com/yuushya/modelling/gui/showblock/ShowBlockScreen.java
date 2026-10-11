package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.gui.AbstractTransformScreen;
import com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader;
import com.yuushya.modelling.gui.widget.BlockStateIconList;
import com.yuushya.modelling.gui.widget.TransformComponent;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static com.yuushya.modelling.blockentity.transformData.TransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;

public class ShowBlockScreen extends AbstractTransformScreen<TransformType, ShowBlockEntity> {
    private static final RandomSource random = RandomSource.create();
    private final BlockState newBlockState;
    private long faceCount = 0;
    private CycleButton<Mode> modeButton;
    private CycleButton<Boolean> shownStateButton;
    private BlockStateIconList blockStateList;
    private Button leftPropertyButton;
    private Property<?> property;
    private Button rightPropertyButton;
    private Button leftStateButton;
    private Button rightStateButton;

    public ShowBlockScreen(ShowBlockEntity blockEntity, BlockState newBlockState) {
        super(GameNarrator.NO_TITLE, blockEntity);
        this.newBlockState = newBlockState;
        if (blockEntity.getSlot() < blockEntity.getTransformData().size()) {
            this.slot = blockEntity.getSlot();
        }
    }

    public static long calculateFaces(ShowBlockEntity showBlockEntity) {
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
        directions.add(null); // 加个null
        return showBlockEntity.getTransformData().stream().mapToInt(
                data -> {
                    int countFace = 0;
                    BlockState blockState = data.blockState;
                    BakedModel blockModel = blockRenderDispatcher.getBlockModel(blockState);
                    for (Direction value : directions) {
                        List<BakedQuad> blockModelQuads = blockModel.getQuads(blockState, value, random);
                        countFace += blockModelQuads.size();
                    }
                    return countFace;
                }
        ).sum();
    }

    @Override
    protected double extract(TransformType type, int slot) {
        return type.extract(this.blockEntity, slot);
    }

    @Override
    protected void modify(TransformType type, int slot, double number) {
        type.modify(this.blockEntity, slot, number);
    }

    @Override
    protected void sendTransform(BlockPos pos, int slot, TransformType type, double number) {
        TransformDataOncePacket.sendToServerSide(pos, slot, type, number);
    }

    @Override
    protected void sendSuccess(BlockPos pos) {
        TransformDataOncePacket.sendToServerSideSuccess(pos);
    }

    @Override
    protected ShareController shareController() {
        return this.share;
    }

    private final ShareController share = new ShareController() {
        @Override
        public String serialize() {
            return ShareUtils.transfer(blockEntity.getTransformData());
        }

        @Override
        public void load(String clipboard) {
            ShareUtils.ShareBlockInformation shareBlockInformation = ShareUtils.from(clipboard);
            if (shareBlockInformation.blocks().isEmpty()) {
                throw new IllegalArgumentException("No block data found");
            }
            checkModLack(shareBlockInformation);
            updateAllTransformData(shareBlockInformation);
            updateStateButtonVisible(true);
        }

        @Override
        public void save(String name) throws IOException {
            EngraveBlockResultLoader.saveBlock(ShareUtils.transfer(blockEntity.getTransformData()), name);
        }
    };

    @Override
    protected void onSlotChanged() {
        shownStateButton.setValue(this.blockEntity.getTransformData(slot).isShown);
        updateStateButtonVisible(true);
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
                                TransformDataOncePacket.sendToServerSideSuccess(blockEntity.getBlockPos());
                                updateStateButtonVisible(true);
                            } else if (this.newBlockState != null) {
                                blockStateList.addSlot();
                                updateTransformDataClient(BLOCK_STATE, (double) Block.getId(this.newBlockState));
                                updateTransformDataClient(SHOWN, 1.0);
                                updateStateButtonVisible(true);
                            }
                        })
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.add")))
                .bounds(RIGHT_COLUMN_X, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
        Button removeStateButton = Button.builder(Component.literal("×"),
                        (btn) -> {
                            updateTransformDataClient(REMOVE, 0.0);
                            updateStateButtonVisible(true);
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.remove")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();
        Button replaceButton = Button.builder(Component.literal("⇄"),
                        (btn) -> {
                            int chosen = this.blockStateList.getChosenOne();
                            if (chosen != -1 && chosen != slot) {
                                updateTransformDataClient(BLOCK_STATE, (double) Block.getId(blockEntity.getTransformData(chosen).blockState));
                                updateStateButtonVisible(true);
                            } else {
                                updateTransformDataClient(BLOCK_STATE, (double) Block.getId(this.newBlockState));
                                updateStateButtonVisible(true);
                            }
                        }
                )
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.display.replace")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH + RIGHT_BAR_WIDTH, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT).build();

        shownStateButton = createShownButton(SHOWN, RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3);


        Button copyButton = createCopyButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3 + 40);
        Button parseButton = createPasteButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 3 + 60);
        Button saveButton = createSaveButton(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 4 + 60);

        blockStateList = new BlockStateIconList(this.minecraft, RIGHT_LIST_WIDTH, RIGHT_LIST_HEIGHT, RIGHT_COLUMN_X, RIGHT_LIST_TOP, RIGHT_LIST_WIDTH, RIGHT_LIST_PER_HEIGHT, this.blockEntity.getTransformData(), this);

        Button statisticButton = Button.builder(Component.literal("📊"),
                        (btn) -> this.faceCount = calculateFaces(blockEntity))
                .tooltip(Tooltip.create(Component.translatable("gui.showBlockScreen.calculate")))
                .bounds(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 5 + 80, TOP, RIGHT_BAR_WIDTH, PER_HEIGHT)
                .build();

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
                            updateTransformDataClient(BLOCK_STATE, (double) Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X, RIGHT_STATE_PANEL_Y + PER_HEIGHT, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();
        rightStateButton = Button.builder(Component.literal(">"),
                        (btn) -> {
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, false);
                            updateTransformDataClient(BLOCK_STATE, (double) Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X + RIGHT_LIST_WIDTH / 2 * 3, RIGHT_STATE_PANEL_Y + PER_HEIGHT, SMALL_BUTTON_WIDTH, PER_HEIGHT)
                .build();

        CycleButton<BlockShape> shapeButton = createShapeButton(SHAPE, SHAPE.extractShape(blockEntity));

        modeButton = createModeButton(Mode.values(), Mode.SLIDER, (btn, mode) -> {
            switch (mode) {
                case SLIDER -> applySliderMode(false, true);
                case FINE_TUNE -> applySliderMode(true, true);
                case EDIT -> applySliderMode(true, false);
            }
        });

        choose(SCALE_X); // 首先放置scala_x, 因为pos_x依赖于它
        double posX = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.x), Math.abs(blockEntity.getTransformData(slot).pos.x));
        double posY = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.y), Math.abs(blockEntity.getTransformData(slot).pos.y));
        double posZ = Math.max(getMaxPos(blockEntity.getTransformData(slot).scales.z), Math.abs(blockEntity.getTransformData(slot).pos.z));
        addTransformRows(List.of(POS_X, POS_Y, POS_Z), List.of(ROT_X, ROT_Y, ROT_Z),
                "gui.yuushya.showBlockScreen.", "block.yuushya.showblock.", posX, posY, posZ);
        addUniformScaleRow(List.of(SCALE_X, SCALE_Y, SCALE_Z), List.of(POS_X, POS_Y, POS_Z),
                Component.empty(), "gui.yuushya.showBlockScreen.scale_text", 6, 10.0);
        addBrightnessRow(LIT, Component.translatable("gui.yuushya.showBlockScreen.brightness_text"), 7);

        registerPanelWidgets();
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
        this.addRenderableWidget(statisticButton);

        blockStateList.setSelectedSlot(slot);//updateStateButtonVisible();
    }

    @Override
    protected boolean isEditMode() {
        return modeButton.getValue() == Mode.EDIT;
    }

    @Override
    protected void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.blockStateList.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderHeader(GuiGraphics guiGraphics) {
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
    }

    @Override
    protected void renderFooter(GuiGraphics guiGraphics) {
        if (faceCount > 0) {
            String text = String.valueOf(faceCount);
            guiGraphics.fill(RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6 + 85, TOP, RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6 + 95 + font.width(text), TOP + 20, 0x80000000);
            guiGraphics.drawString(this.font, Component.literal(text), RIGHT_COLUMN_X + RIGHT_BAR_WIDTH * 6 + 90, TOP + 5, 0xFFFFFFFF, false);
        }
    }

    public void checkModLack(ShareUtils.ShareBlockInformation shareBlockInformation) {
        List<String> unLoaded = shareBlockInformation.mods().stream().filter(id -> !ModList.get().isLoaded(id)).toList();
        Minecraft.getInstance().getToasts().addToast(
                SystemToast.multiline(Minecraft.getInstance(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.literal("Mod Lack"), Component.literal(String.join(", ", unLoaded)))
        );
    }

    private void updateAllTransformData(ShareUtils.ShareBlockInformation shareBlockInformation) {
        List<TransformBlockData> dataList = blockEntity.getTransformData();
        BlockPos pos = blockEntity.getBlockPos();
        int currentSize = dataList.size();
        for (int slot = 0; slot < currentSize; slot++) {
            blockEntity.removeTransformData(slot);
            TransformDataOncePacket.sendToServerSide(pos, slot, REMOVE, 0.0);
        }

        shareBlockInformation.transfer(dataList);

        int nextSize = dataList.size();
        this.blockEntity.getLevel().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.storage.clear();
        for (int slot = 0; slot < nextSize; slot++) {
            TransformBlockData data = dataList.get(slot);
            updateTransformDataServerImmediate(data, slot);
        }
        TransformDataOncePacket.sendToServerSideSuccess(pos);
        for (int slot = nextSize - 1; slot < currentSize; slot++) {
            blockEntity.setSlot(slot);
        }
        this.blockStateList.updateRenderList();
    }

    private void updateTransformDataServerImmediate(TransformBlockData data, int slot) {
        BlockPos pos = blockEntity.getBlockPos();
        TransformDataOncePacket.sendToServerSide(pos, slot, POS_X, data.pos.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, POS_Y, data.pos.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, POS_Z, data.pos.z);

        TransformDataOncePacket.sendToServerSide(pos, slot, ROT_X, data.rot.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, ROT_Y, data.rot.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, ROT_Z, data.rot.z);

        TransformDataOncePacket.sendToServerSide(pos, slot, SCALE_X, data.scales.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Y, data.scales.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, SCALE_Z, data.scales.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, BLOCK_STATE, Block.getId(data.blockState));
        TransformDataOncePacket.sendToServerSide(pos, slot, SHOWN, data.isShown ? 1 : 0);
    }

    public enum Mode implements ScreenMode {
        SLIDER("slider"), FINE_TUNE("fine_tune"), EDIT("edit");

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
