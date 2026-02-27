package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.yuushya.modelling.utils.YuushyaUtils.PROPERTY_ENTRY_TO_STRING_FUNCTION;

public class BlockStateIconList extends ObjectSelectionList<BlockStateIconList.Entry> {

    //private static final Logger LOGGER = LogUtils.getLogger();
    protected final List<TransformBlockData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final ShowBlockScreen screen;
    private final Map<Integer, MutableComponent> rememberDisplayName = new HashMap<>();
    private final Map<Integer, List<String>> rememberBlockStateProperties = new HashMap<>();
    private final Map<Integer, ItemStack> rememberItemStack = new HashMap<>();
    private final Map<Integer, Collection<Property<?>>> rememberProperties = new HashMap<>();
    private int itemHeight;
    private int itemWidth;

    public BlockStateIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                              List<TransformBlockData> transformDataList, ShowBlockScreen showBlockScreen
    ) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.screen = showBlockScreen;
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.updateRenderList();
    }

    public MutableComponent updateRenderDisplayName(BlockState blockState) {
        return rememberDisplayName.computeIfAbsent(Block.getId(blockState),
                (id) -> {
                    BlockState blockState1 = Block.stateById(id);
                    Item item = blockState1.getBlock().asItem();
                    return (item == Items.AIR) ? blockState1.getBlock().getName() : (MutableComponent) item.getName(item.getDefaultInstance());
                });
    }

    public List<String> updateRenderBlockStateProperties(BlockState blockState) {
        return rememberBlockStateProperties.computeIfAbsent(Block.getId(blockState),
                (id) -> Block.stateById(id).getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).toList());
    }

    public ItemStack updateRenderItemstack(BlockState blockState) {
        return rememberItemStack.computeIfAbsent(Block.getId(blockState), (id) -> {
            ItemStack itemStack = ItemRegistry.GET_BLOCKSTATE_ITEM.get().getDefaultInstance();
            itemStack.set(DataComponentRegistry.BLOCKSTATE.get(), Block.stateById(id));
            return itemStack;
        });
    }

    public Collection<Property<?>> updateRenderProperties(BlockState blockState) {
        return rememberProperties.computeIfAbsent(Block.getId(blockState), (id) ->
                Block.stateById(id).getBlock().getStateDefinition().getProperties());
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.getWidth() - 4;
    }

    public void updateRenderList() {
        this.clearEntries();
        for (int i = 0; i < transformDataList.size(); i++) {
            this.addEntry(new Entry(this, i));
        }
        this.notifyListUpdated();
    }

    public void addSlot() {
        if (this.getSelected() != null && transformDataList.get(this.getSelected().slot).blockState.getBlock() == Blocks.AIR) {
            return;
        }
        if (children().size() == transformDataList.size()) {
            Entry entry = new Entry(this, transformDataList.size());
            this.addEntry(entry);
            this.setSelected(entry);
        }
    }

    private void notifyListUpdated() {
    }

    public void setSelectedSlot(int slot) {
        this.setSelected(this.children().get(slot));
    }

    @Override
    public void setSelected(@Nullable BlockStateIconList.Entry selected) {
        super.setSelected(selected);
        if (selected != null) {
            this.screen.setSlot(selected.slot);
        }
    }


    public int getChosenOne() {
        if (!this.chosen.isEmpty()) {
            return chosen.getLast().slot;
        }
        return -1;
    }

    public void setChosenCurrent() {
        Entry selected = this.getSelected();
        if (selected != null) {
            selected.chosen = true;
            this.chosen.add(selected);
        }
    }

    public void clearChosen() {
        for (Entry entry : this.chosen) {
            entry.chosen = false;
        }
        this.chosen.clear();
    }

    public static final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final BlockStateIconList parent;
        private final int slot;
        private final Minecraft minecraft;
        private boolean chosen = false;

        public Entry(BlockStateIconList parent, int slot) {
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        public TransformBlockData getTransformData() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot) : new TransformBlockData();
        }

        public BlockState updateRenderState() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot).blockState : Blocks.AIR.defaultBlockState();
        }

        public boolean updateRenderShown() {
            return parent.transformDataList.size() <= slot || parent.transformDataList.get(slot).isShown;
        }

        @Override
        public @NotNull Component getNarration() {
            BlockState blockState = updateRenderState();
            Item item = blockState.getBlock().asItem();
            return (item == Items.AIR) ? blockState.getBlock().getName() : (MutableComponent) item.getName(item.getDefaultInstance());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Entry preSelected = this.parent.getSelected();
            this.parent.setSelected(this);
            if (preSelected == this) {
                if (this.chosen) {
                    this.chosen = false;
                    this.parent.chosen.remove(this);
                } else {
                    this.chosen = true;
                    this.parent.chosen.add(this);
                }
            }
            //LOGGER.info("select "+this.slot);
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            BlockState blockState = updateRenderState();
            MutableComponent displayName = this.parent.updateRenderDisplayName(blockState);
            Font font = this.minecraft.font;
            int fontHeight = font.lineHeight;
            guiGraphics.drawString(font, displayName, left + 3, top + 32, 0xFFFFFF, false);

            if (updateRenderShown())
                guiGraphics.fill(left, top, left + 32 + 4, top + fontHeight + 32, -1601138544);
            if (chosen)
                guiGraphics.fill(left, top, left + 32 + 4, top + fontHeight + 32, 0x5FD85C2F);
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(left + 8, top + 24, 100);
            pose.scale(16.0f, -16.0f, 16.0f);
            pose.mulPose(Axis.XP.rotationDegrees(30));
            pose.mulPose(Axis.YP.rotationDegrees(45));

            BakedModel blockModel = this.minecraft.getBlockRenderer().getBlockModel(blockState);
            this.minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), guiGraphics.bufferSource().getBuffer(RenderType.TRANSLUCENT), blockState, blockModel, 1.0f, 1.0f, 1.0f, 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

    }
}
