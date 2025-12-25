package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

import static com.yuushya.modelling.client.FontRenderUtil.drawStringUnified;

public class TextBlockEntityRender extends AbstractTransformBlockEntityRender<TextBlockEntity> {
    public TextBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull TextBlockEntity blockEntity, float tickDelta, PoseStack matrixStack,
                       MultiBufferSource multiBufferSource, int light, int overlay) {
        super.render(blockEntity, tickDelta, matrixStack, multiBufferSource, light, overlay);

        Level level = blockEntity.getLevel();
        if (level instanceof ClientLevel clientLevel) {
            for (TransformTextData transformData : blockEntity.getTransformData()) {

                List<String> textLines = transformData.textLines;
                MutableComponent mutableComponent = Component.empty();

                textLines.forEach(line -> {
                    MutableComponent lineComponent = Component.Serializer.fromJson(line, clientLevel.registryAccess());
                    if (lineComponent != null) {
                        mutableComponent.append(lineComponent);
                    }
                });
                matrixStack.pushPose();

                YuushyaUtils.scale(matrixStack, transformData.scales);
                YuushyaUtils.translate(matrixStack, transformData.pos);
                YuushyaUtils.rotate(matrixStack, transformData.rot);

                matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                matrixStack.scale(0.1F, 0.1F, 0.1F);
                Matrix4f matrix4f = matrixStack.last().pose();
                drawStringUnified(font, mutableComponent.getVisualOrderText(), 0, 0, -1, false, matrix4f, multiBufferSource, 0, light);

                matrixStack.popPose();
            }
        }
    }

    @Override
    protected void renderSpecific(TextBlockEntity blockEntity, float tickDelta, PoseStack matrixStack, MultiBufferSource multiBufferSource, int light, int overlay) {
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
        Level level = blockEntity.getLevel();
        {
            Camera camera = this.blockEntityRenderDispatcher.camera;
            renderText(font,
                    Component.translatable("block.yuushya.showblock.pos_text")
                            .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.pos.z)).withStyle(ChatFormatting.BLUE)), 0.8f, matrixStack, multiBufferSource, light, camera);
            renderText(font,
                    Component.translatable("block.yuushya.showblock.rot_text")
                            .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.rot.z())).withStyle(ChatFormatting.BLUE)), 0.55f, matrixStack, multiBufferSource, light, camera);
            renderText(font, Component.translatable("block.yuushya.showblock.scale_text", transformData.scales.x()), 0.3f, matrixStack, multiBufferSource, light, camera);
            float high = 0.3f;
            if (level instanceof ClientLevel clientLevel) {
                for (TransformTextData everyTransformData : blockEntity.getTransformData()) {
                    int slot = blockEntity.getTransformData().indexOf(everyTransformData);

                    List<String> textLines = everyTransformData.textLines;
                    MutableComponent mutableComponent = Component.empty();
                    mutableComponent.append(Component.translatable("block.yuushya.itemblock.slot_text", String.format("%2d", slot)));

                    textLines.forEach(line -> {
                        MutableComponent lineComponent = Component.Serializer.fromJson(line, clientLevel.registryAccess());
                        if (lineComponent != null) {
                            mutableComponent.append(lineComponent);
                        }
                    });
                    renderText(font, mutableComponent, high -= 0.25f, matrixStack, multiBufferSource, light, camera);
                }
            }
        }
        matrixStack.popPose();
    }
}
