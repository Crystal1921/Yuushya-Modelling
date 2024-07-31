package com.yuushya.modelling.blockentity.showblock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import static com.yuushya.modelling.utils.YuushyaUtils.*;

public class ShowBlockEntityRender implements BlockEntityRenderer<ShowBlockEntity> {

    private final Font font;
    public ShowBlockEntityRender(BlockEntityRendererProvider.Context context){ this.font = context.getFont(); }

    public static final Vector3d MIDDLE = new Vector3d(8,8,8);
    public static final Vector3d _MIDDLE = new Vector3d(-8,-8,-8);
    //private final Random random = new Random();
    @Override
    public void render(ShowBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        if(blockEntity.showRotAxis()||blockEntity.showPosAxis()|| blockEntity.showText()){
            TransformData transformData = blockEntity.getTransFormDataNow();
            if(transformData.isShown&&(blockEntity.showPosAxis()||blockEntity.showRotAxis())){
                matrixStack.pushPose();{
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder bufferBuilder = tesselator.getBuilder();
                    RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
                    RenderSystem.depthMask(true);
                    RenderSystem.disableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.lineWidth(8.0f);
                    bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                    translateAfterScale(matrixStack,transformData.pos,transformData.scales);
                    translate(matrixStack,MIDDLE);
                    boolean showRotAxis = blockEntity.showRotAxis();
                    if(showRotAxis) matrixStack.mulPose(Axis.ZP.rotationDegrees(transformData.rot.z()));
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 0.0f, -1.5f).color(90,180, 220,  100).normal(0f,0f,1.5f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 0f, 1.5f).color(90, 180,220,  100).normal(0f,0f,1.5f).endVertex();
                    if(showRotAxis) matrixStack.mulPose(Axis.YP.rotationDegrees(transformData.rot.y()));
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, -1.5f, 0.0f).color(160, 220, 90, 100).normal(0f,1.5f,0f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 1.5f, 0.0f).color(160, 220, 90, 100).normal(0f,1.5f,0f).endVertex();
                    if(showRotAxis) matrixStack.mulPose(Axis.XP.rotationDegrees(transformData.rot.x()));
                    bufferBuilder.vertex(matrixStack.last().pose(),-1.5f, 0.0f, 0.0f).color(230, 90, 70, 100).normal(1.5f,0f,0f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),1.5f, 0f, 0.0f).color(230, 90, 70, 100).normal(1.5f,0f,0f).endVertex();
                    tesselator.end();
                    RenderSystem.depthMask(true);
                    RenderSystem.disableBlend();
                    RenderSystem.enableCull();
                }matrixStack.popPose();
            }
            if(blockEntity.showText()){
                matrixStack.pushPose();{
                    renderText(font,
                            Component.translatable("block.yuushya.showblock.pos_text")
                                    .append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                                    .append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                                    .append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",transformData.pos.z)).withStyle(ChatFormatting.BLUE)),0.8f, matrixStack ,multiBufferSource,light);
                    renderText(font,
                            Component.translatable("block.yuushya.showblock.rot_text")
                                    .append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                                    .append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                                    .append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",transformData.rot.z())).withStyle(ChatFormatting.BLUE)),0.55f, matrixStack ,multiBufferSource,light);
                    renderText(font,Component.translatable("block.yuushya.showblock.scale_text",transformData.scales.x()),0.3f, matrixStack ,multiBufferSource,light);
                    float high=0.3f;
                    for (TransformData everyTransformData : blockEntity.getTransformDatas()) {
                        int slot = blockEntity.getTransformDatas().indexOf(everyTransformData);
                        Style style =  blockEntity.getSlot()==slot ?Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                                : everyTransformData.isShown?Style.EMPTY.withColor( ChatFormatting.WHITE)
                                :Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
                        Block block = everyTransformData.blockState.getBlock();
                        Item item = block.asItem();
                        MutableComponent displayName = (item==Items.AIR) ? block.getName() : (MutableComponent)item.getName(item.getDefaultInstance());
                        Component component = Component.translatable("block.yuushya.showblock.slot_text",String.format("%2d",slot)).append(displayName.append(Component.literal(YuushyaUtils.getBlockStateProperties(everyTransformData.blockState))).withStyle(style));
                        renderText(font,component ,high-=0.25f,matrixStack ,multiBufferSource,light);
                    }
                }matrixStack.popPose();
            }
            blockEntity.consumeShow();
        }

    }

//    public void render(ShowBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
//        BlockPos blockPos = blockEntity.getBlockPos();
//        blockEntity.getTransformDatas().forEach((transformData)->{
//          if(transformData.isShown){
//            matrixStack.pushPose();{
//                scale(matrixStack, transformData.scales);
//                translate(matrixStack,transformData.pos);
//                rotate(matrixStack,transformData.rot);
                /*
                translate(matrixStack,MIDDLE);
                    ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                    itemRenderer.renderStatic(Registry.ITEM.get(new ResourceLocation(Yuushya.MOD_ID,"axis_info")).getDefaultInstance(), ItemTransforms.TransformType.HEAD, light, overlay, matrixStack, multiBufferSource, (int)blockEntity.getBlockPos().asLong());
                    */
//                    BlockState blockState = transformData.blockState;
//                    BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
//                    blockRenderDispatcher.renderBatched(blockState,blockPos,blockEntity.getLevel(),matrixStack,multiBufferSource.getBuffer(RenderType.cutout()),false,random);
//                    blockRenderDispatcher.getModelRenderer().tesselateBlock(blockEntity.getLevel(), blockRenderDispatcher.getBlockModel(blockState), blockState, blockPos, matrixStack, multiBufferSource.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockState)), false, random, blockState.getSeed(blockPos), OverlayTexture.NO_OVERLAY);
//            }matrixStack.popPose();
//        }
//        });
//    }


    public static void renderText(Font font, Component component,float high, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        matrixStack.pushPose();{
            //matrixStack.setIdentity();
            matrixStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            matrixStack.translate(2.0f,  2f+high, 1f);
            matrixStack.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f matrix4f = matrixStack.last().pose();
        float g = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int backgroundColor = (int)(g * 255.0f) << 24;
            float floatx = (float) -font.width(component) / 2;
            font.drawInBatch(component, 0, 0, -1, false, matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, backgroundColor, 0xF000F0);
        }
        matrixStack.popPose();
    }


}
