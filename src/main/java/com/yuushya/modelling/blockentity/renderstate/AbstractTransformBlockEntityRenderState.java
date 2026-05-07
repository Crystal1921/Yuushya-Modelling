package com.yuushya.modelling.blockentity.renderstate;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class AbstractTransformBlockEntityRenderState extends BlockEntityRenderState {
    public boolean isShowFrame;
    public boolean isShowAxis;
    public Direction.Axis showAxis;
    public Direction facing;
}
