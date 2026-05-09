package com.yuushya.modelling.blockentity.renderstate;

import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class TextBlockEntityRenderState extends AbstractTransformBlockEntityRenderState {
    public List<TransformTextData> transformData;
    public List<MutableComponent> textComponents;
}
