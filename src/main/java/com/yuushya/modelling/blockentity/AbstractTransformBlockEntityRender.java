package com.yuushya.modelling.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
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

    /**
     * Renders text with camera-facing orientation
     */
    public static void renderText(Font font, Component component, float high, PoseStack matrixStack,
                                  MultiBufferSource buffer, int packedLight, Camera camera) {
        matrixStack.pushPose();
        {
            Quaternionf quaternion = new Quaternionf().rotationYXZ(-0.017453292F * cameraYRot(camera),
                    0.017453292F * cameraXRot(camera), 0.0F);
            matrixStack.mulPose(quaternion);
            matrixStack.translate(2.0f, 2f + high, 1f);
            matrixStack.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f matrix4f = matrixStack.last().pose();
            float g = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
            int backgroundColor = (int) (g * 255.0f) << 24;
            font.drawInBatch(component, 0, 0, -1, false, matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, backgroundColor, 0xF000F0);
        }
        matrixStack.popPose();
    }

    private static float cameraYRot(Camera camera) {
        return camera.yRot();
    }

    private static float cameraXRot(Camera camera) {
        return camera.xRot();
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
        state.isShowAxis = blockEntity.showRotAxis() || blockEntity.showPosAxis() || blockEntity.showText();
        state.showAxis = blockEntity.getShowAxis();
        state.facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
    }

    @Override
    public void submit(@NotNull AbstractTransformBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
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
     * Renders coordinate axes for rotation and position using the common interface
     */
    protected void renderAxes(AbstractTransformBlockEntityRenderState state, PoseStack matrixStack, MultiBufferSource multiBufferSource,
                              ITransformDataProvider transformData) {
        renderAxes(state, matrixStack, multiBufferSource,
                transformData.getPosition(), transformData.getRotation(), transformData.getScale());
    }

    /**
     * Renders coordinate axes for rotation and position
     */
    protected void renderAxes(AbstractTransformBlockEntityRenderState state, PoseStack matrixStack, MultiBufferSource multiBufferSource,
                              org.joml.Vector3d pos, org.joml.Vector3f rot, org.joml.Vector3f scales) {
        matrixStack.pushPose();
        {
            Direction facing = state.facing;
            float f = facing.toYRot();
            matrixStack.translate(0.5f, 0.5f, 0.5f);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-f));
            matrixStack.translate(-0.5f, -0.5f, -0.5f);

            VertexConsumer bufferBuilder = multiBufferSource.getBuffer(RenderTypes.lines());
            translateAfterScale(matrixStack, pos, scales);
            translate(matrixStack, MIDDLE);

            boolean showRotAxis = state.isShowAxis;
            int redX = 0x64E65A46, greenY = 0x64A0DC5A, blueZ = 0x645AB4DC;

            if (showRotAxis) {
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

            // Render Z axis (blue)
            if (showRotAxis) matrixStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 0.0f, -1.5f).setColor(blueZ).setNormal(0f, 0f, 1.5f);
            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 0f, 1.5f).setColor(blueZ).setNormal(0f, 0f, 1.5f);

            // Render Y axis (green)
            if (showRotAxis) matrixStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, -1.5f, 0.0f).setColor(greenY).setNormal(0f, 1.5f, 0f);
            bufferBuilder.addVertex(matrixStack.last().pose(), 0.0f, 1.5f, 0.0f).setColor(greenY).setNormal(0f, 1.5f, 0f);

            // Render X axis (red)
            if (showRotAxis) matrixStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
            bufferBuilder.addVertex(matrixStack.last().pose(), -1.5f, 0.0f, 0.0f).setColor(redX).setNormal(1.5f, 0f, 0f);
            bufferBuilder.addVertex(matrixStack.last().pose(), 1.5f, 0f, 0.0f).setColor(redX).setNormal(1.5f, 0f, 0f);

            //TODO : 这个轴不知道亮不亮
        }
        matrixStack.popPose();
    }

    /**
     * Abstract method for specific rendering implementation by subclasses
     */
    protected abstract void renderSpecific(@NotNull AbstractTransformBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState);
}