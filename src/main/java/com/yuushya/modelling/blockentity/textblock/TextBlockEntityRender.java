package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.TextBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class TextBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull TextBlockEntity, TextBlockEntityRenderState> {
    private final java.util.LinkedHashMap<String, MutableComponent> componentCacheMap =
            new java.util.LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MutableComponent> eldest) {
                    return size() > 100;
                }
            };

    public TextBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NonNull TextBlockEntityRenderState createRenderState() {
        return new TextBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(@NotNull TextBlockEntity blockEntity, @NotNull TextBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.transformData = blockEntity.getTransformData();
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
    protected void renderSpecific(@NotNull TextBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        List<TransformTextData> transformData = state.transformData;
        if (state.slot >= 0 && state.slot < transformData.size()) {
            TransformTextData transformTextData = transformData.get(state.slot);

            if (state.isShowAxis) {
                renderAxes(state, poseStack, submitNodeCollector, cameraRenderState, transformTextData);
            }

            if (state.isShowText) {
                renderTextInfo(state, poseStack, submitNodeCollector, cameraRenderState, transformTextData);
            }
        }
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

                MutableComponent mutableComponent = componentCacheMap.get(cacheKey);

                if (mutableComponent == null) {
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

    private void renderTextInfo(@NotNull TextBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, TransformTextData transformData) {
        poseStack.pushPose();

        renderTextInfo(Component.translatable("block.yuushya.showblock.pos_text")
                        .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                        .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                        .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.pos.z)).withStyle(ChatFormatting.BLUE)), 0.8f, poseStack, submitNodeCollector, cameraRenderState);
        renderTextInfo(Component.translatable("block.yuushya.showblock.rot_text")
                        .append(Component.translatable("block.yuushya.showblock.x", String.format("%05.1f", transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                        .append(Component.translatable("block.yuushya.showblock.y", String.format("%05.1f", transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                        .append(Component.translatable("block.yuushya.showblock.z", String.format("%05.1f", transformData.rot.z())).withStyle(ChatFormatting.BLUE)), 0.55f, poseStack, submitNodeCollector, cameraRenderState);
        renderTextInfo(Component.translatable("block.yuushya.showblock.scale_text", transformData.scales.x()), 0.3f, poseStack, submitNodeCollector, cameraRenderState);
        float high = 0.3f;
        for (TransformTextData everyTransformData : state.transformData) {
            int slot = state.transformData.indexOf(everyTransformData);

            List<String> textLines = everyTransformData.textLines;
            MutableComponent mutableComponent = Component.empty();
            mutableComponent.append(Component.translatable("block.yuushya.itemblock.slot_text", String.format("%2d", slot)));

            textLines.forEach(line -> {
                MutableComponent lineComponent = Component.Serializer.fromJson(line, Minecraft.getInstance().level.registryAccess());
                if (lineComponent != null) {
                    mutableComponent.append(lineComponent);
                }
            });
            renderTextInfo(mutableComponent, high -= 0.25f, poseStack, submitNodeCollector, cameraRenderState);
        }

        poseStack.popPose();
    }

    private void renderAxes(@NotNull TextBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, TransformTextData transformData) {
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

    @Override
    public boolean shouldRender(@NotNull TextBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return true;
    }
}
