package com.yuushya.modelling.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
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
import org.jspecify.annotations.Nullable;

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
        submitNodeCollector.submitText(poseStack, 0, 0, component.getVisualOrderText(), false, Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, backgroundColor, 0xF000F0, 0);

        poseStack.popPose();
    }

    @Override
    public void extractRenderState(T blockEntity, @NotNull V state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

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
     * Renders coordinate axes for rotation and position based on render state
     */
    protected void renderAxes(@NotNull V state, PoseStack poseStack, CameraRenderState cameraRenderState, org.joml.Vector3d pos, org.joml.Vector3f rot, org.joml.Vector3f scales, boolean showRotAxis) {
        poseStack.pushPose();

        BlockPos blockPos = state.blockPos;
        poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        // Apply facing rotation
        float f = state.facing.toYRot();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-f));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        // Apply transformations
        translateAfterScale(poseStack, pos, scales);
        translate(poseStack, MIDDLE);

        // Calculate axis colors based on showAxis
        int redX = 0x64E65A46, greenY = 0x64A0DC5A, blueZ = 0x645AB4DC;

        if (state.showAxis != null) {
            switch (state.showAxis) {
                case X:
                    redX = 0xFFE65A46;
                    break;
                case Y:
                    greenY = 0xFFA0DC5A;
                    break;
                case Z:
                    blueZ = 0xFF5AB4DC;
                    break;
            }
        }

        // Get current pose matrix for transforming axis positions
        Matrix4f poseMatrix = poseStack.last().pose();

        // Transform origin and axis endpoints
        org.joml.Vector4f origin = new org.joml.Vector4f(0, 0, 0, 1);
        origin.mul(poseMatrix);

        // Z axis (blue)
        org.joml.Vector4f zStart = new org.joml.Vector4f(0, 0, -1.5f, 1);
        org.joml.Vector4f zEnd = new org.joml.Vector4f(0, 0, 1.5f, 1);
        if (showRotAxis) {
            zStart.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
            zEnd.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
        }
        zStart.mul(poseMatrix);
        zEnd.mul(poseMatrix);

        // Y axis (green)
        org.joml.Vector4f yStart = new org.joml.Vector4f(0, -1.5f, 0, 1);
        org.joml.Vector4f yEnd = new org.joml.Vector4f(0, 1.5f, 0, 1);
        if (showRotAxis) {
            yStart.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
            yEnd.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
        }
        if (showRotAxis) {
            yStart.rotateAxis(rot.y() * 0.017453292F, 0, 1, 0);
            yEnd.rotateAxis(rot.y() * 0.017453292F, 0, 1, 0);
        }
        yStart.mul(poseMatrix);
        yEnd.mul(poseMatrix);

        // X axis (red)
        org.joml.Vector4f xStart = new org.joml.Vector4f(-1.5f, 0, 0, 1);
        org.joml.Vector4f xEnd = new org.joml.Vector4f(1.5f, 0, 0, 1);
        if (showRotAxis) {
            xStart.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
            xEnd.rotateAxis(rot.z() * 0.017453292F, 0, 0, 1);
        }
        if (showRotAxis) {
            xStart.rotateAxis(rot.y() * 0.017453292F, 0, 1, 0);
            xEnd.rotateAxis(rot.y() * 0.017453292F, 0, 1, 0);
        }
        if (showRotAxis) {
            xStart.rotateAxis(rot.x() * 0.017453292F, 1, 0, 0);
            xEnd.rotateAxis(rot.x() * 0.017453292F, 1, 0, 0);
        }
        xStart.mul(poseMatrix);
        xEnd.mul(poseMatrix);

        // Render axes using Gizmos
        Gizmos.line(
                new Vec3(origin.x(), origin.y(), origin.z()),
                new Vec3(zEnd.x(), zEnd.y(), zEnd.z()),
                blueZ,
                8.0f
        );

        Gizmos.line(
                new Vec3(origin.x(), origin.y(), origin.z()),
                new Vec3(yEnd.x(), yEnd.y(), yEnd.z()),
                greenY,
                8.0f
        );

        Gizmos.line(
                new Vec3(origin.x(), origin.y(), origin.z()),
                new Vec3(xEnd.x(), xEnd.y(), xEnd.z()),
                redX,
                8.0f
        );

        poseStack.popPose();
    }

    /**
     * Abstract method for specific rendering implementation by subclasses
     */
    protected abstract void renderSpecific(@NotNull V state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState);
}