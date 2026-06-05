package com.yuushya.modelling.blockentity.renderstate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Unified render state for a single slot, supporting both item and block model rendering.
 * Each slot holds either an {@link ItemStackRenderState} or a {@link BlockModelRenderState},
 * determined by {@link #isBlock}.
 */
public class SlotRenderState {
    @Nullable
    public ItemStackRenderState itemState;
    @Nullable
    public BlockModelRenderState blockState;
    public boolean isBlock;

    public static SlotRenderState ofItem(ItemStackRenderState itemState) {
        SlotRenderState state = new SlotRenderState();
        state.itemState = itemState;
        state.isBlock = false;
        return state;
    }

    public static SlotRenderState ofBlock(BlockModelRenderState blockState) {
        SlotRenderState state = new SlotRenderState();
        state.blockState = blockState;
        state.isBlock = true;
        return state;
    }

    /**
     * Submits the render state using the appropriate model type.
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor) {
        if (isBlock && blockState != null) {
            blockState.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
        } else if (itemState != null) {
            itemState.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
        }
    }
}
