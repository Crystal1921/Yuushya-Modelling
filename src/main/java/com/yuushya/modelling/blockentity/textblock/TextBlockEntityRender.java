package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.TextBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.utils.DeprecatedMethod;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull TextBlockEntity, TextBlockEntityRenderState> {
    public static final java.util.LinkedHashMap<String, MutableComponent> componentCacheMap =
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

        Level level = blockEntity.getLevel();
        if (level instanceof ClientLevel clientLevel) {
            ArrayList<MutableComponent> components = new ArrayList<>();
            for (TransformTextData transformData : state.transformData) {
                List<String> textLines = transformData.textLines;
                String cacheKey = String.join("", textLines);

                MutableComponent mutableComponent = componentCacheMap.get(cacheKey);

                if (mutableComponent == null) {
                    final MutableComponent tempComp = Component.empty();
                    textLines.forEach(line -> {
                        MutableComponent lineComponent = DeprecatedMethod.fromJson(line, clientLevel.registryAccess());
                        if (lineComponent != null) {
                            tempComp.append(lineComponent);
                        }
                    });
                    mutableComponent = tempComp;
                    componentCacheMap.put(cacheKey, tempComp);
                }
                components.add(mutableComponent);
            }
            state.textComponents = List.copyOf(components);
        }
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
            ITransformDataProvider transformTextData = transformData.get(state.slot);
            renderAxes(state, poseStack, cameraRenderState, transformTextData.getPosition(), transformTextData.getRotation(), transformTextData.getScale(), true);
        }
    }

    @Override
    public void submit(@NonNull TextBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);

        List<TransformTextData> transformData = state.transformData;
        List<MutableComponent> textComponents = state.textComponents;
        if (transformData.isEmpty() || textComponents == null || textComponents.size() != transformData.size()) {
            return;
        }

        for (int i = 0; i < transformData.size(); i++) {
            TransformTextData transformDatum = transformData.get(i);
            MutableComponent mutableComponent = textComponents.get(i);

            poseStack.pushPose();

            poseStack.translate(0.0D, 1.0D, 0.0D);

            scale(poseStack, transformDatum.scales);
            YuushyaUtils.translate(poseStack, transformDatum.pos);
            rotate(poseStack, transformDatum.rot);

            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(0.1F, 0.1F, 0.1F);

            boolean isCulled = transformDatum.isCulled;
            boolean isMirror = transformDatum.isMirror;

            submitNodeCollector.submitText(poseStack, 0, 0, mutableComponent.getVisualOrderText(), false, Font.DisplayMode.NORMAL, state.lightCoords, 0xFFFFFFFF, 0, 0);

            if (!isCulled) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.translate(-font.width(mutableComponent), 0.0D, 0.0D);
                submitNodeCollector.submitText(poseStack, 0, 0, mutableComponent.getVisualOrderText(), false, Font.DisplayMode.NORMAL, state.lightCoords, 0xFFFFFFFF, 0, 0);
            }

            poseStack.popPose();
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
                MutableComponent lineComponent = DeprecatedMethod.fromJson(line, Minecraft.getInstance().level.registryAccess());
                if (lineComponent != null) {
                    mutableComponent.append(lineComponent);
                }
            });
            renderTextInfo(mutableComponent, high -= 0.25f, poseStack, submitNodeCollector, cameraRenderState);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(@NotNull TextBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return true;
    }
}
