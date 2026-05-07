package com.yuushya.modelling.blockentity.itemblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.renderstate.AbstractTransformBlockEntityRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.rotate;
import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.scale;

/**
 * Renderer for ItemBlockEntity that displays items with transform data.
 * Based on ShowBlockEntityRender but adapted for item data.
 */
public class ItemBlockEntityRender extends AbstractTransformBlockEntityRender<ItemBlockEntity> {

    public ItemBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
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
    protected void renderSpecific(AbstractTransformBlockEntityRenderState state, float tickDelta, PoseStack matrixStack,
                                  MultiBufferSource multiBufferSource, int light, int overlay) {
        TransformItemData transformData = blockEntity.getTransFormDataNow();

        if (transformData.isShown && (blockEntity.showPosAxis() || blockEntity.showRotAxis())) {
            renderAxes(blockEntity, matrixStack, multiBufferSource, transformData);
        }

        if (blockEntity.showText()) {
            renderTextInfo(blockEntity, transformData, matrixStack, multiBufferSource, light);
        }
    }

    private void renderTextInfo(ItemBlockEntity blockEntity, TransformItemData transformData,
                                PoseStack matrixStack, MultiBufferSource multiBufferSource, int light) {
        matrixStack.pushPose();
        {
            Camera camera = this.blockEntityRenderDispatcher.camera;

            // Render position information
            renderText(font,
                    Component.translatable("block.yuushya.itemblock.pos_text")
                            .append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", transformData.pos.z)).withStyle(ChatFormatting.BLUE)),
                    0.8f, matrixStack, multiBufferSource, light, camera);

            // Render rotation information
            renderText(font,
                    Component.translatable("block.yuushya.itemblock.rot_text")
                            .append(Component.translatable("block.yuushya.itemblock.x", String.format("%05.1f", transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                            .append(Component.translatable("block.yuushya.itemblock.y", String.format("%05.1f", transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                            .append(Component.translatable("block.yuushya.itemblock.z", String.format("%05.1f", transformData.rot.z())).withStyle(ChatFormatting.BLUE)),
                    0.55f, matrixStack, multiBufferSource, light, camera);

            // Render scale information
            renderText(font, Component.translatable("block.yuushya.itemblock.scale_text", transformData.scales.x()),
                    0.3f, matrixStack, multiBufferSource, light, camera);

            // Render item slot information
            float high = 0.3f;
            for (TransformItemData everyTransformData : blockEntity.getTransformData()) {
                int slot = blockEntity.getTransformData().indexOf(everyTransformData);
                Style style = blockEntity.getSlot() == slot ? Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                        : everyTransformData.isShown ? Style.EMPTY.withColor(ChatFormatting.WHITE)
                        : Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);

                ItemStack itemStack = everyTransformData.itemStack;
                MutableComponent displayName = itemStack.isEmpty() ?
                        Component.literal("Air") :
                        (MutableComponent) itemStack.getDisplayName();

                Component component = Component.translatable("block.yuushya.itemblock.slot_text", String.format("%2d", slot))
                        .append(displayName.withStyle(style));
                renderText(font, component, high -= 0.25f, matrixStack, multiBufferSource, light, camera);
            }
        }
        matrixStack.popPose();
    }
}