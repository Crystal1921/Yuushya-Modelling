package com.yuushya.modelling.blockentity.renderstate;

import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.List;

public class ItemBlockEntityRenderState extends AbstractTransformBlockEntityRenderState {
    public List<TransformItemData> transformData;
    public List<ItemStackRenderState> renderData;
    public boolean enableSpecialRender;
}
