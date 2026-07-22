package com.yuushya.modelling.blockentity.itemblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.ItemBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.SlotRenderState;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.rotate;
import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.scale;

/**
 * Renderer for ItemBlockEntity that displays items with transform data.
 * Based on ShowBlockEntityRender but adapted for item data.
 */
public class ItemBlockEntityRender extends AbstractTransformBlockEntityRender<@NotNull ItemBlockEntity, ItemBlockEntityRenderState> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final ItemModelResolver itemModelResolver;
    private final BlockModelResolver blockModelResolver;


    public ItemBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.itemModelResolver();
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public @NonNull ItemBlockEntityRenderState createRenderState() {
        return new ItemBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(@NotNull ItemBlockEntity blockEntity, @NotNull ItemBlockEntityRenderState state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.transformData = blockEntity.getTransformData();
        state.enableSpecialRender = blockEntity.getBlockState().getValue(AbstractTransformBlock.ENABLE_SPECIAL_RENDER);
        Level level = blockEntity.getLevel();
        ArrayList<SlotRenderState> renderStates = new ArrayList<>();
        for (TransformItemData transformDatum : state.transformData) {
            BlockState blockState = transformDatum.itemStack.get(DataComponentRegistry.BLOCKSTATE);
            if (transformDatum.enableBlock && blockState != null) {
                BlockModelRenderState blockModelRenderState = new BlockModelRenderState();
                blockModelResolver.update(blockModelRenderState, blockState, BLOCK_DISPLAY_CONTEXT);
                renderStates.add(SlotRenderState.ofBlock(blockModelRenderState));
            } else {
                ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
                this.itemModelResolver.updateForTopItem(itemStackRenderState, transformDatum.itemStack, ItemDisplayContext.NONE, level, null, (int) (blockEntity.getBlockPos().asLong()));
                renderStates.add(SlotRenderState.ofItem(itemStackRenderState));
            }
        }
        state.renderData = List.copyOf(renderStates);
    }

    @Override
    public void submit(@NonNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
        if (state.enableSpecialRender) {
            renderItemBlock(state, poseStack, submitNodeCollector, cameraRenderState);
        }
    }

    private void renderItemBlock(@NonNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        List<TransformItemData> transformData = state.transformData;
        List<SlotRenderState> renderData = state.renderData;
        if (transformData.isEmpty() || renderData.isEmpty() || renderData.size() != transformData.size()) {
            return;
        }

        for (int i = 0; i < transformData.size(); i++) {
            TransformItemData transformDatum = transformData.get(i);
            if (transformDatum.isShown) {
                ItemStack itemStack = transformDatum.itemStack;
                if (!itemStack.isEmpty()) {
                    poseStack.pushPose();
                    {
                        poseStack.translate(0.5, 0.5, 0.5);
                        YuushyaUtils.translate(poseStack, transformDatum.pos);
                        rotate(poseStack, transformDatum.rot);
                        scale(poseStack, transformDatum.scales);
                        SlotRenderState slotRenderState = renderData.get(i);
                        slotRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    }
                    poseStack.popPose();
                }
            }
        }
    }

    @Override
    protected void renderSpecific(@NotNull ItemBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        List<TransformItemData> transformData = state.transformData;
        if (state.slot >= 0 && state.slot < transformData.size()) {
            ITransformDataProvider transformItemData = transformData.get(state.slot);
            renderAxes(state, poseStack, cameraRenderState, transformItemData.getPosition(), transformItemData.getRotation(), transformItemData.getScale(), true);
        }

        if (state.isShowText) {
            TransformItemData transformDataNow = state.transformData.get(state.slot);
            renderTextInfo(state, poseStack, submitNodeCollector, cameraRenderState, transformDataNow);
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
                    (MutableComponent) itemStack.getDisplayName().copy();
            Component component = Component.translatable("block.yuushya.itemblock.slot_text", String.format("%2d", slot)).append(displayName.withStyle(style));
            renderTextInfo(component, high -= 0.25f, poseStack, submitNodeCollector, cameraRenderState);
        }

        poseStack.popPose();
    }
}
