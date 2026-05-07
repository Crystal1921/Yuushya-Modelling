package com.yuushya.modelling.blockentity.renderstate;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public abstract class AbstractTransformBlockEntityRenderState extends BlockEntityRenderState {
    public boolean isShowFrame;
    public boolean isShowAxis;
    public boolean isShowText;
    public int slot;
    public Direction.Axis showAxis;
    public Direction facing;
}
