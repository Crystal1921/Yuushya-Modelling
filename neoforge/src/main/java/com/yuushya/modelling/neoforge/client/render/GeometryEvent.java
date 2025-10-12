//package com.yuushya.modelling.neoforge.client.render;
//
//import com.google.common.base.Function;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.mojang.math.Axis;
//import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
//import com.yuushya.modelling.blockentity.transformData.TransformItemData;
//import com.yuushya.modelling.utils.YuushyaUtils;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.client.renderer.LightTexture;
//import net.minecraft.client.renderer.RenderStateShard;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.entity.ItemRenderer;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.client.resources.model.BakedModel;
//import net.minecraft.client.resources.model.BuiltInModel;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.item.ItemDisplayContext;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.chunk.LevelChunk;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//import java.util.Map;
//
//import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
//
//@EventBusSubscriber
//public class GeometryEvent {
//    @SubscribeEvent
//    public static void SectionGeometryEvent(AddSectionGeometryEvent event) {
//        if (event.getLevel() instanceof ClientLevel clientLevel) {
//            Minecraft mc = Minecraft.getInstance();
//            LocalPlayer player = mc.player;
//            if (player == null) {
//                return;
//            }
//            BlockPos origin = event.getSectionOrigin();
//            LevelChunk chunk = event.getLevel().getChunkAt(origin);
//            List<ItemBlockEntity> list = chunk.getBlockEntities().values()
//                    .stream()
//                    .filter(be -> be instanceof ItemBlockEntity)
//                    .map(be -> (ItemBlockEntity) be)
//                    .toList();
//            event.addRenderer(new CustomItemRenderer(mc.getItemRenderer(), clientLevel, list, player));
//        }
//    }
//
//    static class CustomItemRenderer implements AddSectionGeometryEvent.AdditionalSectionRenderer {
//        final ItemRenderer itemRenderer;
//        final ClientLevel clientLevel;
//        final List<ItemBlockEntity> list;
//        final LocalPlayer player;
//
//        CustomItemRenderer(ItemRenderer itemRenderer, ClientLevel clientLevel, List<ItemBlockEntity> list, LocalPlayer player) {
//            this.itemRenderer = itemRenderer;
//            this.clientLevel = clientLevel;
//            this.list = list;
//            this.player = player;
//        }
//
//        @Override
//        public void render(AddSectionGeometryEvent.@NotNull SectionRenderingContext sectionRenderingContext) {
//            for (ItemBlockEntity itemBlockEntity : list) {
//                BlockPos blockPos = itemBlockEntity.getBlockPos();
//                Direction value = itemBlockEntity.getBlockState().getValue(HORIZONTAL_FACING);
//                List<TransformItemData> transformDatas = itemBlockEntity.getTransformData();
//                float f = value.toYRot();
//                PoseStack stack = new PoseStack();
//                stack.translate(0.5f, 0.5f, 0.5f);
//                stack.mulPose(Axis.YP.rotationDegrees(-f));
//                stack.translate(-0.5f, -0.5f, -0.5f);
//                transformDatas.forEach(transformData -> {
//                    if (transformData.isShown) {
//                        ItemStack itemStack = transformData.itemStack;
//                        BakedModel blockModel = itemRenderer.getModel(itemStack, null, null, player.getId());
//                        for (BakedModel model : blockModel.getRenderPasses(itemStack, true)) {
//                            if (model instanceof BuiltInModel builtInModel) {
//                                MeshBufferSource recorder = new MeshBufferSource();
//                                stack.pushPose();
//                                {
//                                    YuushyaUtils.scale(stack, transformData.scales);
//                                    YuushyaUtils.translate(stack, transformData.pos);
//                                    YuushyaUtils.rotate(stack, transformData.rot);
//                                    itemRenderer.render(itemStack, ItemDisplayContext.NONE, false, stack, recorder, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY, model);
//
//                                    Map<RenderType, List<Vertex>> freeze = recorder.freeze();
//
//                                    freeze.forEach((type, values) -> {
//                                        if (type instanceof RenderType.CompositeRenderType compositeRenderType && compositeRenderType.state().textureState instanceof RenderStateShard.TextureStateShard textureStateShard) {
//                                            textureStateShard.cutoutTexture().ifPresent(resourceLocation -> {
//                                                VertexConsumer orCreateChunkBuffer = sectionRenderingContext.getOrCreateChunkBuffer(RenderType.cutoutMipped());
//                                                values.forEach(vertex -> {
//                                                            orCreateChunkBuffer.addVertex(vertex.x(), vertex.y(), vertex.z())
//                                                                    .setColor(vertex.colorARGB())
//                                                                    .setUv(vertex.u(), vertex.v())
//                                                                    .setNormal(vertex.nx(), vertex.ny(), vertex.nz())
//                                                                    .setUv1(getUv1U(vertex.overlayPacked()), getUv1V(vertex.overlayPacked()))
//                                                                    .setUv2(getUv2U(vertex.lightPacked()), getUv2V(vertex.lightPacked()));
//                                                        }
//                                                );
//                                            });
//                                        }
//
//                                    });
//
//                                }
//                            }
//                        }
//                    }
//                });
//            }
//        }
//    }
//
//    private static int getUv1U(int overlayPacked) {
//        return overlayPacked & 0xFFFF; // 低16位
//    }
//
//    private static int getUv1V(int overlayPacked) {
//        return (overlayPacked >> 16) & 0xFFFF; // 高16位
//    }
//
//    private static int getUv2U(int lightPacked) {
//        return lightPacked & 0xFFFF;
//    }
//
//    private static int getUv2V(int lightPacked) {
//        return (lightPacked >> 16) & 0xFFFF;
//    }
//}
