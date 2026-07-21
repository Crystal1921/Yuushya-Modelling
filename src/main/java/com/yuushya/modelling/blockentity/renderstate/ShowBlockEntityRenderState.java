package com.yuushya.modelling.blockentity.renderstate;

import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import net.minecraft.client.renderer.block.BlockModelRenderState;

import java.util.ArrayList;
import java.util.List;

public class ShowBlockEntityRenderState extends AbstractTransformBlockEntityRenderState{
    public List<TransformBlockData> transformData;
    public List<BlockModelRenderState> blockModelRenderStates = new ArrayList<>();
}
