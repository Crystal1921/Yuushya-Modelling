package com.yuushya.modelling.blockentity.renderstate;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public abstract class AbstractTransformBlockEntityRenderState extends BlockEntityRenderState {
    public boolean isShowFrame;
    public boolean isShowAxis;
    public boolean isShowText;
    public int slot;
    public Direction.@Nullable Axis highlightedAxis;
    public Direction facing;
}
