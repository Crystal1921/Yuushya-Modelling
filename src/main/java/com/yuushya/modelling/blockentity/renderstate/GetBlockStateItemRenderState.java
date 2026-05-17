package com.yuushya.modelling.blockentity.renderstate;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class GetBlockStateItemRenderState {
    public boolean isEmpty;
    public final BlockModelRenderState carriedBlock = new BlockModelRenderState();
    public final ItemStackRenderState defaultItemRenderState = new ItemStackRenderState();
}
