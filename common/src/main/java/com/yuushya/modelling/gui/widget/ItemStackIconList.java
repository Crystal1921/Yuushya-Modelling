package com.yuushya.modelling.gui.widget;

import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStackIconList extends ObjectSelectionList<ItemStackIconList.Entry> {

    protected final List<TransformItemData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final ItemBlockScreen screen;
    private final Map<Integer, MutableComponent> rememberDisplayName = new HashMap<>();
    private final Map<Integer, List<String>> rememberItemProperties = new HashMap<>();
    private int itemHeight;
    private int itemWidth;

    public ItemStackIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                             List<TransformItemData> transformDataList, ItemBlockScreen itemBlockScreen) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.screen = itemBlockScreen;
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.updateRenderList();
    }

    public MutableComponent updateRenderDisplayName(ItemStack itemStack) {
        return rememberDisplayName.computeIfAbsent(Item.getId(itemStack.getItem()),
                (id) -> {
                    Item item = Item.byId(id);
                    return (MutableComponent) item.getName(item.getDefaultInstance());
                });
    }

    public List<String> updateRenderItemProperties(ItemStack itemStack) {
        return rememberItemProperties.computeIfAbsent(Item.getId(itemStack.getItem()),
                (id) -> {
                    // For items, we can show basic properties like count, damage, etc.
                    List<String> properties = new ArrayList<>();
                    Item item = Item.byId(id);
                    if (item != Items.AIR) {
                        properties.add("Count: " + itemStack.getCount());
                        if (itemStack.isDamageableItem()) {
                            properties.add("Damage: " + itemStack.getDamageValue() + "/" + itemStack.getMaxDamage());
                        }
                    }
                    return properties;
                });
    }

    public void updateRenderList() {
        this.clearEntries();
        for (int i = 0; i < transformDataList.size(); i++) {
            Entry entry = new Entry(this, i);
            this.addEntry(entry);
        }
    }

    public void addSlot() {
        if (this.getSelected() != null && transformDataList.get(this.getSelected().slot).itemStack.isEmpty()) {
            return;
        }
        if (children().size() == transformDataList.size()) {
            ItemStackIconList.Entry entry = new ItemStackIconList.Entry(this, transformDataList.size());
            this.addEntry(entry);
            this.setSelected(entry);
        }
    }

    // getChosenOne 返回最后选中的 Entry 的 index
    public int getChosenOne() {
        if (!this.chosen.isEmpty()) {
            return this.chosen.getLast().slot;
        }
        return -1;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < this.chosen.size()) {
            this.setSelected(this.chosen.get(slot));
        }
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.getWidth() - 4;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {

        private final ItemStackIconList parent;
        private final int slot;
        private final Minecraft minecraft;
        private boolean chosen = false;

        public Entry(ItemStackIconList parent, int slot) {
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        public TransformItemData getTransformData() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot) : new TransformItemData();
        }

        public ItemStack updateRenderState() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot).itemStack : Items.AIR.getDefaultInstance();
        }

        public boolean updateRenderShown() {
            return parent.transformDataList.size() <= slot || parent.transformDataList.get(slot).isShown;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ItemStack itemStack = updateRenderState();

            // Render item icon
            if (!itemStack.isEmpty()) {
                BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, null, null, 0);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + itemWidth / 2.0f, y + itemHeight / 2.0f, 100.0f);
                guiGraphics.pose().scale(16.0f, -16.0f, 16.0f);
                boolean flatItem = !bakedModel.usesBlockLight();
                if (flatItem) {
                    guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(180.0f));
                }
                Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false,
                        guiGraphics.pose(), guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
                guiGraphics.pose().popPose();
            }

            // Render index number
            Font font = Minecraft.getInstance().font;
            String indexStr = String.valueOf(this.slot);
            int textX = x + itemWidth - font.width(indexStr) - 2;
            int textY = y + 2;
            guiGraphics.drawString(font, indexStr, textX, textY, 0xFFFFFF, true);

            // Render selection indicator
            if (isMouseOver || this == ItemStackIconList.this.getSelected()) {
                guiGraphics.fill(x - 1, y - 1, x + itemWidth + 1, y + itemHeight + 1, 0x80FFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ItemStackIconList.Entry preSelected = this.parent.getSelected();
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
        public Component getNarration() {
            return Component.translatable("narrator.select", this.slot);
        }
    }
}