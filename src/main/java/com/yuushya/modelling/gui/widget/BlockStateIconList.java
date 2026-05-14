package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class BlockStateIconList extends ObjectSelectionList<BlockStateIconList.Entry> {

    //private static final Logger LOGGER = LogUtils.getLogger();
    protected final List<TransformBlockData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final ShowBlockScreen screen;
    private final Map<Integer, MutableComponent> rememberDisplayName = new HashMap<>();
    private final Map<Integer, List<String>> rememberBlockStateProperties = new HashMap<>();
    private final Map<Integer, ItemStack> rememberItemStack = new HashMap<>();
    private final Map<Integer, Collection<Property<?>>> rememberProperties = new HashMap<>();
    private final int itemHeight;
    private final int itemWidth;

    public BlockStateIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                              List<TransformBlockData> transformDataList, ShowBlockScreen showBlockScreen
    ) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.screen = showBlockScreen;
        this.centerListVertically = false;
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
                (id) -> Block.stateById(id).getValues().map(Property.Value::toString).toList());
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
    protected int scrollBarX() {
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
        public @NonNull Component getNarration() {
            BlockState blockState = updateRenderState();
            Item item = blockState.getBlock().asItem();
            return (item == Items.AIR) ? blockState.getBlock().getName() : (MutableComponent) item.getName(item.getDefaultInstance());
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
        public void extractContent(GuiGraphicsExtractor guiGraphics, int x, int y, boolean hovered, float v) {
            BlockState blockState = updateRenderState();
            MutableComponent displayName = this.parent.updateRenderDisplayName(blockState);
            Font font = this.minecraft.font;
            int fontHeight = font.lineHeight;
            guiGraphics.text(font, displayName, getX() + 3, getY() + 32, 0xFFFFFF);

            if (updateRenderShown())
                guiGraphics.fill(getX(), getY() + 1, getX() + 39, getY() + fontHeight + 35, -1601138544);
            if (chosen)
                guiGraphics.fill(getX(),getY() + 1, getX() + 39, getY() + fontHeight + 35, 0x5FD85C2F);

            if (Minecraft.getInstance().level != null) {
                guiGraphics.item(blockState.getBlock().asItem().getDefaultInstance(), getX() + 8, getY() + 24);
            }
        }

    }
}
