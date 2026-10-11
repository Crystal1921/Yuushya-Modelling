package com.yuushya.modelling.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class ItemStackIconList extends AbstractTransformIconList<TransformItemData, ItemStackIconList.Entry> {

    protected final ItemBlockScreen screen;

    public ItemStackIconList(Minecraft minecraft, int width, int height, int x, int y0, int itemWidth, int itemHeight,
                             List<TransformItemData> transformDataList, ItemBlockScreen itemBlockScreen) {
        super(minecraft, width, height, x, y0, itemWidth, itemHeight, transformDataList);
        this.screen = itemBlockScreen;
    }

    @Override
    protected Entry createEntry(int slot) {
        return new Entry(this, slot);
    }

    @Override
    protected TransformItemData defaultData() {
        return new TransformItemData();
    }

    @Override
    protected boolean isSlotEmpty(int slot) {
        return transformDataList.get(slot).itemStack.isEmpty();
    }

    @Override
    protected void selectSlot(int slot) {
        this.screen.setSlot(slot);
    }

    public class Entry extends AbstractTransformIconList.Entry<TransformItemData, Entry> {

        // Animation state for the popup effect
        private boolean animating = false;
        private long animStartMs = 0L;
        private static final long PHASE_MS = 80L;
        private static final long TOTAL_MS = PHASE_MS * 2L;
        private float cachedScaleX = 1f;
        private float cachedScaleY = 1f;
        private float cachedScaleZ = 1f;

        public Entry(ItemStackIconList parent, int slot) {
            super(parent, slot);
        }

        public ItemStack updateRenderState() {
            return (parent.transformDataList.size() > slot) ? parent.transformDataList.get(slot).itemStack : Items.AIR.getDefaultInstance();
        }

        private void startPopupAnimation() {
            TransformItemData td = getTransformData();
            // Cache current scales (use the transform data's values at click start)
            this.cachedScaleX = td.scales.x();
            this.cachedScaleY = td.scales.y();
            this.cachedScaleZ = td.scales.z();
            this.animStartMs = System.currentTimeMillis();
            this.animating = true;

//            if (!level.isClientSide){
//                level.playSound(null,blockPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS,1f,0.2f);
//            }
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
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int itemWidth, int itemHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            // Advance and apply animation (if running) BEFORE rendering so the preview sees updated values
            applyAnimationIfNeeded();

            ItemStack itemStack = updateRenderState();

            // Render item icon
            if (!itemStack.isEmpty()) {
                BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, null, null, 0);
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(x + itemWidth / 2.0f, y + itemHeight / 2.0f, 100.0f);
                pose.scale(24.0f, -24.0f, 24.0f);

                boolean flatItem = !bakedModel.usesBlockLight();
                if (flatItem) {
                    pose.mulPose(Axis.YP.rotationDegrees(180.0f));
                }
                Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false,
                        pose, guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
                pose.popPose();
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
        protected void onFirstSelected() {
            // Start popup animation when clicked (indicate selection);
            // the base class then calls screen.setSlot(this.slot)
            startPopupAnimation();
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.slot);
        }
    }
}