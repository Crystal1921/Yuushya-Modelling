package com.yuushya.modelling.blockentity.itemblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.ItemBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.rotate;
import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.scale;

/**
 * Renderer for ItemBlockEntity that displays items with transform data.
 * Based on ShowBlockEntityRender but adapted for item data.
 */
public class ItemBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull ItemBlockEntity, ItemBlockEntityRenderState> {

    public ItemBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NonNull ItemBlockEntityRenderState createRenderState() {
        return new ItemBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(@NotNull ItemBlockEntity blockEntity, @NotNull ItemBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.transformData = blockEntity.getTransformData();
    }

    @Override
    public void render(@NotNull ItemBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack,
                       @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        super.render(blockEntity, tickDelta, matrixStack, multiBufferSource, light, overlay);
        if (blockEntity.getBlockState().getValue(AbstractTransformBlock.ENABLE_SPECIAL_RENDER)) {
            renderItemBlock(blockEntity, tickDelta, matrixStack, multiBufferSource, light, overlay);
        }
    }

    private void renderItemBlock(@NotNull ItemBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource bufferSource, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        List<TransformItemData> transformDatas = blockEntity.getTransformData();

        for (TransformItemData transformData : transformDatas)
            if (transformData.isShown) {
                ItemStack itemStack = transformData.itemStack;
                if (!itemStack.isEmpty()) {
                    matrixStack.pushPose();
                    {
                        matrixStack.translate(0.5,0.5,0.5);
                        scale(matrixStack, transformData.scales);
                        YuushyaUtils.translate(matrixStack, transformData.pos);
                        rotate(matrixStack, transformData.rot);
                        mc.getItemRenderer().renderStatic(itemStack, ItemDisplayContext.NONE, light, overlay, matrixStack, bufferSource, blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());
                    }
                    matrixStack.popPose();
                }
            }
    }

    @Override
    protected void renderSpecific(@NotNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        List<TransformItemData> transformData = state.transformData;
        if (state.slot >= 0 && state.slot < transformData.size()) {
            TransformItemData transformItemData = transformData.get(state.slot);

            if (state.isShowAxis) {
                renderAxes(state, poseStack, submitNodeCollector, cameraRenderState, transformItemData);
            }

            if (state.isShowText) {
                renderTextInfo(state, poseStack, submitNodeCollector, cameraRenderState, transformItemData);
            }
        }
    }

    private void renderTextInfo(@NotNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, TransformItemData transformData) {
        poseStack.pushPose();

        renderTextInfo(Component.translatable("block.yuushya.itemblock.pos_text")
                        .append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                        .append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                        .append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", transformData.pos.z)).withStyle(ChatFormatting.BLUE)), 0.8f, poseStack, submitNodeCollector, cameraRenderState);
        renderTextInfo(Component.translatable("block.yuushya.itemblock.rot_text")
                        .append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                        .append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                        .append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", transformData.rot.z())).withStyle(ChatFormatting.BLUE)), 0.55f, poseStack, submitNodeCollector, cameraRenderState);
        renderTextInfo(Component.translatable("block.yuushya.itemblock.scale_text", transformData.scales.x()), 0.3f, poseStack, submitNodeCollector, cameraRenderState);
        float high = 0.3f;
        for (TransformItemData everyTransformData : state.transformData) {
            int slot = state.transformData.indexOf(everyTransformData);
            Style style = state.slot == slot ? Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                    : everyTransformData.isShown ? Style.EMPTY.withColor(ChatFormatting.WHITE)
                    : Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
            ItemStack itemStack = everyTransformData.itemStack;
            MutableComponent displayName = itemStack.isEmpty() ?
                    Component.literal("Air") :
                    (MutableComponent) itemStack.getDisplayName();
            Component component = Component.translatable("block.yuushya.itemblock.slot_text", String.format("%2d", slot)).append(displayName.withStyle(style));
            renderTextInfo(component, high -= 0.25f, poseStack, submitNodeCollector, cameraRenderState);
        }

        poseStack.popPose();
    }

    private void renderAxes(@NotNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, TransformItemData transformData) {
        poseStack.pushPose();
        {
            Direction facing = state.facing;
            float f = facing.toYRot();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(-f));
            poseStack.translate(-0.5f, -0.5f, -0.5f);

            float redX = 0.39f, greenX = 0.35f, blueX = 0.27f;
            float redY = 0.63f, greenY = 0.86f, blueY = 0.35f;
            float redZ = 0.35f, greenZ = 0.71f, blueZ = 0.86f;

            if (state.isShowAxis) {
                switch (state.showAxis) {
                    case X:
                        redX = 1.0f; greenX = 0.35f; blueX = 0.27f;
                        break;
                    case Y:
                        redY = 0.63f; greenY = 0.86f; blueY = 0.35f;
                        break;
                    case Z:
                        redZ = 0.35f; greenZ = 0.71f; blueZ = 0.86f;
                        break;
                }
            }

            poseStack.pushPose();
            {
                YuushyaUtils.translateAfterScale(poseStack, transformData.pos, transformData.scales);
                YuushyaUtils.translate(poseStack, AbstractTransformBlockEntityRender.MIDDLE);

                Vector3f rot = transformData.rot;

                poseStack.pushPose();
                if (state.isShowAxis) poseStack.mulPose(Axis.ZP.rotationDegrees(rot.z()));
                Vec3 zStart = transformPoint(poseStack, 0.0f, 0.0f, -1.5f);
                Vec3 zEnd = transformPoint(poseStack, 0.0f, 0.0f, 1.5f);
                poseStack.popPose();

                poseStack.pushPose();
                if (state.isShowAxis) poseStack.mulPose(Axis.YP.rotationDegrees(rot.y()));
                Vec3 yStart = transformPoint(poseStack, 0.0f, -1.5f, 0.0f);
                Vec3 yEnd = transformPoint(poseStack, 0.0f, 1.5f, 0.0f);
                poseStack.popPose();

                poseStack.pushPose();
                if (state.isShowAxis) poseStack.mulPose(Axis.XP.rotationDegrees(rot.x()));
                Vec3 xStart = transformPoint(poseStack, -1.5f, 0.0f, 0.0f);
                Vec3 xEnd = transformPoint(poseStack, 1.5f, 0.0f, 0.0f);
                poseStack.popPose();

                Gizmos.line(zStart, zEnd, ARGB.colorFromFloat(0.39f, redZ, greenZ, blueZ));
                Gizmos.line(yStart, yEnd, ARGB.colorFromFloat(0.39f, redY, greenY, blueY));
                Gizmos.line(xStart, xEnd, ARGB.colorFromFloat(0.39f, redX, greenX, blueX));
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private Vec3 transformPoint(PoseStack poseStack, float x, float y, float z) {
        Matrix4f matrix = poseStack.last().pose();
        org.joml.Vector4f vec = new org.joml.Vector4f(x, y, z, 1.0f);
        matrix.transform(vec);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }
}
