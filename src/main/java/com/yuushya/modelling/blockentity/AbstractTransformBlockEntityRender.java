package com.yuushya.modelling.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.ShowBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.yuushya.modelling.utils.YuushyaUtils.translate;
import static com.yuushya.modelling.utils.YuushyaUtils.translateAfterScale;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * Abstract base class for transform block entity renderers that provides common functionality
 * for rendering frames, axes, and text.
 */
public abstract class AbstractTransformBlockEntityRender<T extends AbstractTransformBlockEntity, V extends AbstractTransformBlockEntityRenderState> implements BlockEntityRenderer<T, V> {

    public static final Vector3d MIDDLE = new Vector3d(8, 8, 8);
    public static final Vector3d _MIDDLE = new Vector3d(-8, -8, -8);

    protected final Font font;
    protected final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public AbstractTransformBlockEntityRender(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
        this.blockEntityRenderDispatcher = context.blockEntityRenderDispatcher();
    }

    public static void renderTextInfo(Component component, float high, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();

        Quaternionf quaternion = new Quaternionf().rotationYXZ(-0.017453292F * cameraRenderState.yRot,
                0.017453292F * cameraRenderState.xRot, 0.0F);
        poseStack.mulPose(quaternion);
        poseStack.translate(2.0f, 2f + high, 1f);
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        float g = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int backgroundColor = (int) (g * 255.0f) << 24;
        submitNodeCollector.submitText(poseStack, 0,0, component.getVisualOrderText(), false, Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, backgroundColor, 0xF000F0, 0);

        poseStack.pushPose();
    }

    @Override
    public void extractRenderState(T blockEntity, @NotNull V state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.showFrame()) {
            blockEntity.consumeShowFrame();
        }
        if (state.isShowAxis) {
            blockEntity.consumeShow();
            blockEntity.consumeShowAxis();
        }
        state.isShowFrame = blockEntity.showFrame();
        state.isShowText = blockEntity.showText();
        state.isShowAxis = blockEntity.showRotAxis() || blockEntity.showPosAxis() || blockEntity.showText();
        state.slot = blockEntity.slot;
        state.showAxis = blockEntity.getShowAxis();
        state.facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
    }

    @Override
    public void submit(@NotNull V state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state.isShowFrame) {
            Gizmos.cuboid(
                    new AABB(0, 0, 0, 1, 1, 1).move(state.blockPos),
                    GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.9F, 0.9F, 0.9F)),
                    true
            );
        }

        if (state.isShowAxis) {
            renderSpecific(state, poseStack, submitNodeCollector, cameraRenderState);
        }
    }

    /**
     * Abstract method for specific rendering implementation by subclasses
     */
    protected abstract void renderSpecific(@NotNull V state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState);
}