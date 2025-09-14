package com.yuushya.modelling.blockentity.showblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntityRender;
import com.yuushya.modelling.blockentity.TransformBlockData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;

public class ShowBlockEntityRender extends AbstractTransformBlockEntityRender<ShowBlockEntity> {

    public ShowBlockEntityRender(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSpecific(ShowBlockEntity blockEntity, float tickDelta, PoseStack matrixStack, 
                                 MultiBufferSource multiBufferSource, int light, int overlay) {
        TransformBlockData transformData = blockEntity.getTransFormDataNow();
        
        if (transformData.isShown && (blockEntity.showPosAxis() || blockEntity.showRotAxis())) {
            renderAxes(blockEntity, matrixStack, multiBufferSource, transformData);
        }
        
        if (blockEntity.showText()) {
            renderTextInfo(blockEntity, transformData, matrixStack, multiBufferSource, light);
        }
    }

    private void renderTextInfo(ShowBlockEntity blockEntity, TransformBlockData transformData, 
                               PoseStack matrixStack, MultiBufferSource multiBufferSource, int light) {
        matrixStack.pushPose();
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
            for (TransformBlockData everyTransformData : blockEntity.getTransformData()) {
                int slot = blockEntity.getTransformData().indexOf(everyTransformData);
                Style style = blockEntity.getSlot() == slot ? Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                        : everyTransformData.isShown ? Style.EMPTY.withColor(ChatFormatting.WHITE)
                        : Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
                Block block = everyTransformData.blockState.getBlock();
                Item item = block.asItem();
                MutableComponent displayName = (item == Items.AIR) ? block.getName() : (MutableComponent) item.getName(item.getDefaultInstance());
                Component component = Component.translatable("block.yuushya.showblock.slot_text", String.format("%2d", slot)).append(displayName.append(Component.literal(YuushyaUtils.getBlockStateProperties(everyTransformData.blockState))).withStyle(style));
                renderText(font, component, high -= 0.25f, matrixStack, multiBufferSource, light, camera);
            }
        }
        matrixStack.popPose();
    }
}
