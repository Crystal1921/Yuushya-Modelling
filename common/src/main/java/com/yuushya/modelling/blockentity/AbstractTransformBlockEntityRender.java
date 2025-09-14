package com.yuushya.modelling.blockentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import static com.yuushya.modelling.utils.YuushyaUtils.translate;
import static com.yuushya.modelling.utils.YuushyaUtils.translateAfterScale;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * Abstract base class for transform block entity renderers that provides common functionality
 * for rendering frames, axes, and text.
 */
public abstract class AbstractTransformBlockEntityRender<T extends AbstractTransformBlockEntity> implements BlockEntityRenderer<T> {

    public static final Vector3d MIDDLE = new Vector3d(8, 8, 8);
    public static final Vector3d _MIDDLE = new Vector3d(-8, -8, -8);
    
    protected final Font font;
    protected final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public AbstractTransformBlockEntityRender(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.blockEntityRenderDispatcher = context.getBlockEntityRenderDispatcher();
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
        return camera.getYRot();
    }

    private static float cameraXRot(Camera camera) {
        return camera.getXRot();
    }

    /**
     * Renders the frame outline around the block
     */
    protected void renderFrame(T blockEntity, PoseStack matrixStack, MultiBufferSource multiBufferSource) {
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, -0.01, -0.01, -0.01, 1.01, 1.01, 1.01, 
                                  1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
        blockEntity.consumeShowFrame();
    }

    /**
     * Renders coordinate axes for rotation and position using the common interface
     */
    protected void renderAxes(T blockEntity, PoseStack matrixStack, MultiBufferSource multiBufferSource, 
                            ITransformDataProvider transformData) {
        renderAxes(blockEntity, matrixStack, multiBufferSource, 
                  transformData.getPosition(), transformData.getRotation(), transformData.getScale());
    }

    /**
     * Renders coordinate axes for rotation and position
     */
    protected void renderAxes(T blockEntity, PoseStack matrixStack, MultiBufferSource multiBufferSource, 
                            org.joml.Vector3d pos, org.joml.Vector3f rot, org.joml.Vector3f scales) {
        matrixStack.pushPose();
        {
            Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
            float f = facing.toYRot();
            matrixStack.translate(0.5f, 0.5f, 0.5f);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-f));
            matrixStack.translate(-0.5f, -0.5f, -0.5f);
            
            Tesselator tesselator = Tesselator.getInstance();
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(8.0f);
            
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            translateAfterScale(matrixStack, pos, scales);
            translate(matrixStack, MIDDLE);
            
            boolean showRotAxis = blockEntity.showRotAxis();
            int redX = 0x64E65A46, greenY = 0x64A0DC5A, blueZ = 0x645AB4DC;
            
            if (blockEntity.getShowAxis() != null) {
                switch (blockEntity.getShowAxis()) {
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
            
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
        matrixStack.popPose();
    }

    @Override
    public void render(T blockEntity, float tickDelta, @NotNull PoseStack matrixStack, 
                      @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        // Render frame if requested
        if (blockEntity.showFrame()) {
            renderFrame(blockEntity, matrixStack, multiBufferSource);
        }
        
        // Handle axis and text rendering (implemented by subclasses)
        if (blockEntity.showRotAxis() || blockEntity.showPosAxis() || blockEntity.showText()) {
            renderSpecific(blockEntity, tickDelta, matrixStack, multiBufferSource, light, overlay);
            blockEntity.consumeShow();
            blockEntity.consumeShowAxis();
        }
    }

    /**
     * Abstract method for specific rendering implementation by subclasses
     */
    protected abstract void renderSpecific(T blockEntity, float tickDelta, PoseStack matrixStack, 
                                         MultiBufferSource multiBufferSource, int light, int overlay);
}