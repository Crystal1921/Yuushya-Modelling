package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
//        this.setRenderHeader(false, 0);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.updateRenderList();
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

    @Override
    public void setSelected(@Nullable ItemStackIconList.Entry selected) {
        super.setSelected(selected);
        if (selected != null) {
            this.screen.setSlot(selected.slot);
        }
    }

    public void setSelectedSlot(int slot) {
        this.setSelected(this.children().get(slot));
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    protected int scrollBarX() {
        return this.getX() + this.getWidth() - 4;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {

        private final ItemStackIconList parent;
        private final int slot;
        private final Minecraft minecraft;
        private boolean chosen = false;

        // Animation state for the popup effect
        private boolean animating = false;
        private long animStartMs = 0L;
        private static final long PHASE_MS = 80L;
        private static final long TOTAL_MS = PHASE_MS * 2L;
        private float cachedScaleX = 1f;
        private float cachedScaleY = 1f;
        private float cachedScaleZ = 1f;

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

        private void startPopupAnimation() {
            TransformItemData td = getTransformData();
            // Cache current scales (use the transform data's values at click start)
            this.cachedScaleX = td.scales.x();
            this.cachedScaleY = td.scales.y();
            this.cachedScaleZ = td.scales.z();
            this.animStartMs = System.currentTimeMillis();
            this.animating = true;
        }

        private void applyAnimationIfNeeded() {
            if (!this.animating) return;

            long elapsed = System.currentTimeMillis() - this.animStartMs;

            if (elapsed >= TOTAL_MS) {
                // End animation, restore cached scale via ItemBlockScreen.updateTransformData
                // Use ItemTransformType to update SCALE_X/Y/Z back to cached values.
                screen.updateTransformDataClient(ItemTransformType.SCALE_X, (double) this.cachedScaleX);
                screen.updateTransformDataClient(ItemTransformType.SCALE_Y, (double) this.cachedScaleY);
                screen.updateTransformDataClient(ItemTransformType.SCALE_Z, (double) this.cachedScaleZ);
                this.animating = false;
                return;
            }

            float scaleFactor;
            if (elapsed < PHASE_MS) {
                // First phase: grow from 1.0 to 1.1 linearly
                float p = (float) elapsed / (float) PHASE_MS;
                scaleFactor = 1.0f + 0.1f * p; // 1.0 -> 1.1
            } else {
                // Second phase: shrink from 1.1 back to 1.0 linearly
                float p = (float) (elapsed - PHASE_MS) / (float) PHASE_MS;
                scaleFactor = 1.1f - 0.1f * p; // 1.1 -> 1.0
            }

            // Apply intermediate scale via ItemBlockScreen.updateTransformData so the block preview updates
            screen.updateTransformDataClient(ItemTransformType.SCALE_X, (double) (this.cachedScaleX * scaleFactor));
            screen.updateTransformDataClient(ItemTransformType.SCALE_Y, (double) (this.cachedScaleY * scaleFactor));
            screen.updateTransformDataClient(ItemTransformType.SCALE_Z, (double) (this.cachedScaleZ * scaleFactor));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            ItemStackIconList.Entry previousSelected = this.parent.getSelected();
            if (previousSelected != this) {
                // Start popup animation when clicked (indicate selection)
                // setSelected above will call screen.setSlot(selected.slot) so the screen's current slot will be this.slot
                startPopupAnimation();
            }

            this.parent.setSelected(this);

            if (previousSelected == this) {
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

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int x, int y, boolean hovered, float v) {
            applyAnimationIfNeeded();

            ItemStack itemStack = updateRenderState();

            // Render item icon
            if (!itemStack.isEmpty()) {
                guiGraphics.item(itemStack, (int) (getX() + itemWidth / 2.0f) - 8, (int) (getY() + itemHeight / 2.0f) - 8);
                //TODO 不知道这样渲染对不对
//                BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, null, null, 0);
//                PoseStack pose = guiGraphics.pose();
//                pose.pushPose();
//                pose.translate(x + itemWidth / 2.0f, y + itemHeight / 2.0f, 100.0f);
//                pose.scale(24.0f, -24.0f, 24.0f);
//
//                boolean flatItem = !bakedModel.usesBlockLight();
//                if (flatItem) {
//                    pose.mulPose(Axis.YP.rotationDegrees(180.0f));
//                }
//                Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false,
//                        pose, guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
//                pose.popPose();
            }

            // Render index number
            Font font = Minecraft.getInstance().font;
            String indexStr = String.valueOf(this.slot);
            int textX = getX() + itemWidth - font.width(indexStr) - 2;
            int textY = getY() + 2;
            guiGraphics.text(font, indexStr, textX, textY, 0xFFFFFF, true);

            // Render selection indicator
            if (hovered || this == ItemStackIconList.this.getSelected()) {
                guiGraphics.fill(getX() - 1, getY() - 1, getX() + itemWidth + 1, getY() + itemHeight + 1, 0x80FFFFFF);
            }
        }
    }
}