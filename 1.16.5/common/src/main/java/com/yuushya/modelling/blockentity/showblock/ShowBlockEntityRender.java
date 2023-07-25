package com.yuushya.modelling.blockentity.showblock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import static com.yuushya.modelling.utils.YuushyaUtils.*;

public class ShowBlockEntityRender extends BlockEntityRenderer<ShowBlockEntity> {

    private final Font font;
    public ShowBlockEntityRender(BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
        super(blockEntityRenderDispatcher);
        this.font = blockEntityRenderDispatcher.getFont();
    }

    public static final Vec3 MIDDLE = new Vec3(8,8,8);
    public static final Vec3 _MIDDLE = new Vec3(-8,-8,-8);

    //private final Random random = new Random();
    @Override
    public void render(ShowBlockEntity blockEntity, float tickDelta, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        if(blockEntity.showRotAxis()||blockEntity.showPosAxis()|| blockEntity.showText()){
            TransformData transformData = blockEntity.getTransFormDataNow();
            if(transformData.isShown&&(blockEntity.showPosAxis()||blockEntity.showRotAxis())){
                matrixStack.pushPose();{
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder bufferBuilder = tesselator.getBuilder();
                    RenderSystem.depthMask(true);
                    RenderSystem.disableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableTexture();
                    RenderSystem.lineWidth(8.0f);
                    bufferBuilder.begin(GL11.GL_LINE, DefaultVertexFormat.POSITION_COLOR);
                    translate(matrixStack,transformData.pos);
                    translate(matrixStack,MIDDLE);
                    boolean showRotAxis = blockEntity.showRotAxis();
                    if(showRotAxis) matrixStack.mulPose(Vector3f.ZP.rotationDegrees(transformData.rot.z()));
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 0.0f, -1.5f).color(90,180, 220,  100).normal(0f,0f,1.5f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 0f, 1.5f).color(90, 180,220,  100).normal(0f,0f,1.5f).endVertex();
                    if(showRotAxis) matrixStack.mulPose(Vector3f.YP.rotationDegrees(transformData.rot.y()));
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, -1.5f, 0.0f).color(160, 220, 90, 100).normal(0f,1.5f,0f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),0.0f, 1.5f, 0.0f).color(160, 220, 90, 100).normal(0f,1.5f,0f).endVertex();
                    if(showRotAxis) matrixStack.mulPose(Vector3f.XP.rotationDegrees(transformData.rot.x()));
                    bufferBuilder.vertex(matrixStack.last().pose(),-1.5f, 0.0f, 0.0f).color(230, 90, 70, 100).normal(1.5f,0f,0f).endVertex();
                    bufferBuilder.vertex(matrixStack.last().pose(),1.5f, 0f, 0.0f).color(230, 90, 70, 100).normal(1.5f,0f,0f).endVertex();
                    tesselator.end();
                    RenderSystem.depthMask(true);
                    RenderSystem.disableBlend();
                    RenderSystem.enableCull();
                    RenderSystem.enableTexture();
                }matrixStack.popPose();
            }
            if(blockEntity.showText()){
                matrixStack.pushPose();{
                    renderText(font,
                            new TranslatableComponent("block.yuushya.showblock.pos_text")
                                    .append(new TranslatableComponent("block.yuushya.showblock.x",String.format("%05.1f",transformData.pos.x)).withStyle(ChatFormatting.DARK_RED))
                                    .append(new TranslatableComponent("block.yuushya.showblock.y",String.format("%05.1f",transformData.pos.y)).withStyle(ChatFormatting.GREEN))
                                    .append(new TranslatableComponent("block.yuushya.showblock.z",String.format("%05.1f",transformData.pos.z)).withStyle(ChatFormatting.BLUE)),0.8f, matrixStack ,multiBufferSource,light);
                    renderText(font,
                            new TranslatableComponent("block.yuushya.showblock.rot_text")
                                    .append(new TranslatableComponent("block.yuushya.showblock.x",String.format("%05.1f",transformData.rot.x())).withStyle(ChatFormatting.DARK_RED))
                                    .append(new TranslatableComponent("block.yuushya.showblock.y",String.format("%05.1f",transformData.rot.y())).withStyle(ChatFormatting.GREEN))
                                    .append(new TranslatableComponent("block.yuushya.showblock.z",String.format("%05.1f",transformData.rot.z())).withStyle(ChatFormatting.BLUE)),0.55f, matrixStack ,multiBufferSource,light);
                    renderText(font,new TranslatableComponent("block.yuushya.showblock.scale_text",transformData.scales.x()),0.3f, matrixStack ,multiBufferSource,light);
                    float high=0.3f;
                    for (TransformData everyTransformData : blockEntity.getTransformDatas()) {
                        int slot = blockEntity.getTransformDatas().indexOf(everyTransformData);
                        Style style =  blockEntity.getSlot()==slot ?Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                                : everyTransformData.isShown?Style.EMPTY.withColor( ChatFormatting.WHITE)
                                :Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
                        Component component = new TranslatableComponent("block.yuushya.showblock.slot_text",String.format("%2d",slot)).append(everyTransformData.blockState.getBlock().getName().append(new TextComponent(YuushyaUtils.getBlockStateProperties(everyTransformData.blockState))).withStyle(style));
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
            float floatx = -font.width(component) / 2;
            font.drawInBatch(component, 0, 0, -1, false, matrix4f, buffer, false, backgroundColor, 0xF000F0);
        }
        matrixStack.popPose();
    }


}
