package com.yuushya.modelling.blockentity.itemblock;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

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
}
