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
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemStackIconList extends ObjectSelectionList<ItemStackIconList.Entry> {

    protected final List<TransformItemData> transformDataList;
    protected final List<Entry> chosen = new ArrayList<>();
    protected final ItemBlockScreen screen;
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

    private final Map<Integer, MutableComponent> rememberDisplayName = new HashMap<>();
    public MutableComponent updateRenderDisplayName(ItemStack itemStack) {
        return rememberDisplayName.computeIfAbsent(Item.getId(itemStack.getItem()),
                (id) -> {
                    Item item = Item.byId(id);
                    return (MutableComponent) item.getName(item.getDefaultInstance());
                });
    }

    private final Map<Integer, List<String>> rememberItemProperties = new HashMap<>();
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
        this.chosen.clear();
        for (int i = 0; i < transformDataList.size(); i++) {
            Entry entry = new Entry(transformDataList.get(i), i);
            this.chosen.add(entry);
            this.addEntry(entry);
        }
    }

    public void addSlot() {
        Entry entry = new Entry(new TransformItemData(), transformDataList.size());
        this.chosen.add(entry);
        this.addEntry(entry);
    }

    public int getChosenOne() {
        return this.getSelected() != null ? this.chosen.indexOf(this.getSelected()) : -1;
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
        return this.x1 - 6;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final TransformItemData transformData;
        private final int index;

        Entry(TransformItemData transformData, int index) {
            this.transformData = transformData;
            this.index = index;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                          int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            ItemStack itemStack = transformData.itemStack;
            
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
            String indexStr = String.valueOf(this.index);
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
            ItemStackIconList.this.setSelected(this);
            screen.setSlot(this.index);
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.index);
        }
    }
}