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

    // TODO : AI改的，需要检查
    public void renderAxes(@NotNull ShowBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, List<TransformBlockData> transformData) {
        poseStack.pushPose();
        {
            Direction facing = state.facing;
            float f = facing.toYRot();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(-f));
            poseStack.translate(-0.5f, -0.5f, -0.5f);

            // Calculate color alpha based on which axis is selected
            float redX = 0.39f, greenX = 0.35f, blueX = 0.27f; // 0x64E65A46
            float redY = 0.63f, greenY = 0.86f, blueY = 0.35f; // 0x64A0DC5A
            float redZ = 0.35f, greenZ = 0.71f, blueZ = 0.86f; // 0x645AB4DC

            if (state.isShowAxis) {
                switch (state.showAxis) {
                    case X:
                        redX = 1.0f; greenX = 0.35f; blueX = 0.27f; // 0xFFE65A46
                        break;
                    case Y:
                        redY = 0.63f; greenY = 0.86f; blueY = 0.35f; // 0xFFA0DC5A
                        break;
                    case Z:
                        redZ = 0.35f; greenZ = 0.71f; blueZ = 0.86f; // 0xFF5AB4DC
                        break;
                }
            }

            for (TransformBlockData data : transformData) {
                poseStack.pushPose();
                {
                    translateAfterScale(poseStack, data.pos, data.scales);
                    translate(poseStack, MIDDLE);

                    Vector3f rot = data.rot;

                    // Calculate transformed axis endpoints
                    Vec3 origin = new Vec3(0, 0, 0);

                    // Z axis (blue)
                    poseStack.pushPose();
                    if (state.isShowAxis) poseStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
                    Vec3 zStart = transformPoint(poseStack, 0.0f, 0.0f, -1.5f);
                    Vec3 zEnd = transformPoint(poseStack, 0.0f, 0.0f, 1.5f);
                    poseStack.popPose();

                    // Y axis (green)
                    poseStack.pushPose();
                    if (state.isShowAxis) poseStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
                    Vec3 yStart = transformPoint(poseStack, 0.0f, -1.5f, 0.0f);
                    Vec3 yEnd = transformPoint(poseStack, 0.0f, 1.5f, 0.0f);
                    poseStack.popPose();

                    // X axis (red)
                    poseStack.pushPose();
                    if (state.isShowAxis) poseStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
                    Vec3 xStart = transformPoint(poseStack, -1.5f, 0.0f, 0.0f);
                    Vec3 xEnd = transformPoint(poseStack, 1.5f, 0.0f, 0.0f);
                    poseStack.popPose();

                    // Render axes with calculated colors
                    Gizmos.line(zStart, zEnd, ARGB.colorFromFloat(0.39f, redZ, greenZ, blueZ));
                    Gizmos.line(yStart, yEnd, ARGB.colorFromFloat(0.39f, redY, greenY, blueY));
                    Gizmos.line(xStart, xEnd, ARGB.colorFromFloat(0.39f, redX, greenX, blueX));
                }
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /**
     * Transforms a point using the current pose stack matrix
     */
    private Vec3 transformPoint(PoseStack poseStack, float x, float y, float z) {
        Matrix4f matrix = poseStack.last().pose();
        org.joml.Vector4f vec = new org.joml.Vector4f(x, y, z, 1.0f);
        matrix.transform(vec);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    //    /**
//     * Renders coordinate axes for rotation and position using the common interface
//     */
//    protected void renderAxes(V state, PoseStack matrixStack, MultiBufferSource multiBufferSource,
//                              ITransformDataProvider transformData) {
//        renderAxes(state, matrixStack, multiBufferSource,
//                transformData.getPosition(), transformData.getRotation(), transformData.getScale());
//    }
//
//    /**
//     * Renders coordinate axes for rotation and position
//     */
//    protected void renderAxes(V state, PoseStack matrixStack, MultiBufferSource multiBufferSource,
//                              Vector3d pos, Vector3f rot, Vector3f scales) {
//        matrixStack.pushPose();
//        {
//            Direction facing = state.facing;
//            float f = facing.toYRot();
//            matrixStack.translate(0.5f, 0.5f, 0.5f);
//            matrixStack.mulPose(Axis.YP.rotationDegrees(-f));
//            matrixStack.translate(-0.5f, -0.5f, -0.5f);
//
//            VertexConsumer bufferBuilder = multiBufferSource.getBuffer(RenderTypes.lines());
//            translateAfterScale(matrixStack, pos, scales);
//            translate(matrixStack, MIDDLE);
//
//            boolean showRotAxis = state.isShowAxis;
//            int redX = 0x64E65A46, greenY = 0x64A0DC5A, blueZ = 0x645AB4DC;
//
//            if (showRotAxis) {
//                switch (state.showAxis) {
//                    case X:
//                        redX = 0xFFE65A46;
//                        break;
//                    case Y:
//                        greenY = 0xFFA0DC5A;
//                        break;
//                    case Z:
//                        blueZ = 0xFF5AB4DC;
//                        break;
//                }
//            }
//
//            // Render Z axis (blue)
//            if (showRotAxis) matrixStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
//            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 0.0f, -1.5f).setColor(blueZ).setNormal(0f, 0f, 1.5f);
//            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 0f, 1.5f).setColor(blueZ).setNormal(0f, 0f, 1.5f);
//
//            // Render Y axis (green)
//            if (showRotAxis) matrixStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
//            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, -1.5f, 0.0f).setColor(greenY).setNormal(0f, 1.5f, 0f);
//            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 1.5f, 0.0f).setColor(greenY).setNormal(0f, 1.5f, 0f);
//
//            // Render X axis (red)
//            if (showRotAxis) matrixStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
//            bufferBuilder.addVertex(matrixStack.last().pose(), -1.5f, 0.0f, 0.0f).setColor(redX).setNormal(1.5f, 0f, 0f);
//            bufferBuilder.addVertex(matrixStack.last().pose(), 1.5f, 0f, 0.0f).setColor(redX).setNormal(1.5f, 0f, 0f);
//
//        }
//        matrixStack.popPose();
//    }

    /**
     * Abstract method for specific rendering implementation by subclasses
     */
    protected abstract void renderSpecific(@NotNull V state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState);
}