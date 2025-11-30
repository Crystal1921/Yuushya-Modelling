package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Renderer for ItemBlockEntity that displays items with transform data.
 * Based on ShowBlockEntityRender but adapted for item data.
 */
public class ItemBlockEntityRender extends AbstractTransformBlockEntityRender<ItemBlockEntity> {

    public ItemBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSpecific(ItemBlockEntity blockEntity, float tickDelta, PoseStack matrixStack, 
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