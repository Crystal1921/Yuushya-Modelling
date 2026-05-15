package com.yuushya.modelling.blockentity.showblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.ShowBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShowBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull ShowBlockEntity, ShowBlockEntityRenderState> {

    public ShowBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSpecific(@NotNull ShowBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        List<TransformBlockData> transformData = state.transformData;
        if (state.slot >= 0 && state.slot < transformData.size()) {
            ITransformDataProvider transformTextData = transformData.get(state.slot);

        }
    }

    @Override
    public ShowBlockEntityRenderState createRenderState() {
        return new ShowBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(@NotNull ShowBlockEntity blockEntity, @NotNull ShowBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.transformData = blockEntity.getTransformData();
    }

    private void renderTextInfo(@NotNull ShowBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, TransformBlockData transformData) {
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
        for (TransformBlockData everyTransformData : state.transformData) {
            int slot = state.transformData.indexOf(everyTransformData);
            Style style = state.slot == slot ? Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                    : everyTransformData.isShown ? Style.EMPTY.withColor(ChatFormatting.WHITE)
                    : Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
            Block block = everyTransformData.blockState.getBlock();
            Item item = block.asItem();
            MutableComponent displayName = (item == Items.AIR) ? block.getName() : (MutableComponent) item.getName(item.getDefaultInstance());
            Component component = Component.translatable("block.yuushya.showblock.slot_text", String.format("%2d", slot)).append(displayName.append(Component.literal(YuushyaUtils.getBlockStateProperties(everyTransformData.blockState))).withStyle(style));
            renderTextInfo(component, high -= 0.25f, poseStack, submitNodeCollector, cameraRenderState);
        }

        poseStack.popPose();
    }
}
