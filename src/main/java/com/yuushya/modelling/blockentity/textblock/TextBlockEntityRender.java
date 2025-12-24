package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class TextBlockEntityRender extends AbstractTransformBlockEntityRender<TextBlockEntity> {

    public TextBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSpecific(TextBlockEntity blockEntity, float tickDelta, PoseStack matrixStack,
                                  MultiBufferSource multiBufferSource, int light, int overlay) {
        TransformTextData transformData = blockEntity.getTransFormDataNow();

        if (transformData.isShown && (blockEntity.showPosAxis() || blockEntity.showRotAxis())) {
            renderAxes(blockEntity, matrixStack, multiBufferSource, transformData);
        }

        if (blockEntity.showText()) {
            renderTextInfo(blockEntity, transformData, matrixStack, multiBufferSource, light);
        }
    }

    private void renderTextInfo(TextBlockEntity blockEntity, TransformTextData transformData,
                                PoseStack matrixStack, MultiBufferSource multiBufferSource, int light) {
        matrixStack.pushPose();
        {
            Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
            float f = facing.toYRot();
            matrixStack.translate(0.5f, 0.5f, 0.5f);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-f));
            matrixStack.translate(-0.5f, -0.5f, -0.5f);
            YuushyaUtils.scale(matrixStack, transformData.scales);
            YuushyaUtils.translate(matrixStack, transformData.pos);
            YuushyaUtils.rotate(matrixStack, transformData.rot);

            Camera camera = this.blockEntityRenderDispatcher.camera;
            renderText(font,
                    Component.translatable("block.yuushya.showblock.pos_text")
                            .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.pos.z)).withStyle(ChatFormatting.BLUE)),
                    0.8f, matrixStack, multiBufferSource, light, camera);
            renderText(font,
                    Component.translatable("block.yuushya.showblock.rot_text")
                            .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.rot.z())).withStyle(ChatFormatting.BLUE)),
                    0.55f, matrixStack, multiBufferSource, light, camera);
            renderText(font, Component.translatable("block.yuushya.showblock.scale_text", transformData.scales.x()),
                    0.3f, matrixStack, multiBufferSource, light, camera);

            Level level = blockEntity.getLevel();
            if (level != null) {
                float high = 0.3f;
                for (String line : transformData.textLines) {
                    Component component = null;
                    try {
                        component = Component.Serializer.fromJson(line, level.registryAccess());
                    } catch (Exception ex) {
                        Yuushya.LOGGER.warn("Failed to parse text component for TextBlockEntity at {}: {}", blockEntity.getBlockPos(), line, ex);
                    }
                    if (component != null) {
                        high -= 0.25f;
                        renderText(font, component, high, matrixStack, multiBufferSource, light, camera);
                    }
                }
            }
        }
        matrixStack.popPose();
    }
}
