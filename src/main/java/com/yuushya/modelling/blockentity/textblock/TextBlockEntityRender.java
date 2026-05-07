package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class TextBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull TextBlockEntity> {
    private final java.util.LinkedHashMap<String, MutableComponent> componentCacheMap =
            new java.util.LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MutableComponent> eldest) {
                    return size() > 100;  // 限制最多100个条目
                }
            };


    public TextBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static void scale(PoseStack arg, Vector3f scales) {
        if (scales.x() != 1 || scales.y() != 1 || scales.z() != 1) {
            arg.scale(scales.x(), scales.y(), scales.z());
        }
    }

    public static void rotate(PoseStack arg, Vector3f rot) {
        float roll = rot.z(), yaw = rot.y(), pitch = rot.x();
        if (roll != 0.0F || yaw != 0.0F || pitch != 0.0F) {
            if (roll != 0.0F)
                arg.mulPose(Axis.ZP.rotationDegrees(roll));
            if (yaw != 0.0F)
                arg.mulPose(Axis.YP.rotationDegrees(yaw));
            if (pitch != 0.0F)
                arg.mulPose(Axis.XP.rotationDegrees(pitch));
        }
    }

    @Override
    protected void renderSpecific(@NotNull AbstractTransformBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {

    }

    @Override
    public void render(@NotNull TextBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack,
                       @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        super.render(blockEntity, tickDelta, matrixStack, multiBufferSource, light, overlay);

        Level level = blockEntity.getLevel();
        if (level instanceof ClientLevel clientLevel) {
            for (TransformTextData transformData : blockEntity.getTransformData()) {

                List<String> textLines = transformData.textLines;
                String cacheKey = String.join("", textLines);

                // 尝试从缓存获取
                MutableComponent mutableComponent = componentCacheMap.get(cacheKey);

                if (mutableComponent == null) {
                    // 重新构建并缓存
                    final MutableComponent tempComp = Component.empty();
                    textLines.forEach(line -> {
                        MutableComponent lineComponent = Component.Serializer.fromJson(line, clientLevel.registryAccess());
                        if (lineComponent != null) {
                            tempComp.append(lineComponent);
                        }
                    });
                    mutableComponent = tempComp;
                    componentCacheMap.put(cacheKey, tempComp);
                }

                matrixStack.pushPose();

                matrixStack.translate(0.0D, 1.0D, 0.0D);

                scale(matrixStack, transformData.scales);
                YuushyaUtils.translate(matrixStack, transformData.pos);
                rotate(matrixStack, transformData.rot);

                matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                matrixStack.scale(0.1F, 0.1F, 0.1F);
                Matrix4f matrix4f = matrixStack.last().pose();

                boolean isCulled = transformData.isCulled;
                boolean isMirror = transformData.isMirror;

                if (!isCulled && !isMirror) {
                    drawStringUnified(font, mutableComponent.getVisualOrderText(), 0, 0, -1, false, matrix4f, multiBufferSource, 0, light);
                }

                if (!isCulled && isMirror) {
                    font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4f, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
                    matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    matrixStack.translate(-font.width(mutableComponent), 0.0D, 0.0D);
                    Matrix4f matrix4fMirror = matrixStack.last().pose();
                    font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4fMirror, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
                }

                if (isCulled) {
                    font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4f, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
                }


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

    @Override
    public @NotNull AbstractTransformBlockEntityRenderState createRenderState() {
        return null;
    }

    @Override
    public boolean shouldRender(@NotNull TextBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return true;
    }
}
