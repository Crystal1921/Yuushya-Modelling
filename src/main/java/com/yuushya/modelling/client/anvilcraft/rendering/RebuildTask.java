package com.yuushya.modelling.client.anvilcraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.renderstate.ItemBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.SlotRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState.EMPTY_TINTS;

public class RebuildTask implements Runnable {
    private final CachedRenderingChunk cachedRenderingChunk;
    private boolean cancelled = false;

    public RebuildTask(CachedRenderingChunk cachedRenderingChunk) {
        this.cachedRenderingChunk = cachedRenderingChunk;
    }

    @Override
    public void run() {
        Minecraft minecraft = Minecraft.getInstance();

        cachedRenderingChunk.setLastRebuildTask(this);
        PoseStack poseStack = new PoseStack();
        cachedRenderingChunk.setEmpty(true);
        FullyBufferedBufferSource bufferSource = new FullyBufferedBufferSource();

        // 缓存几何是静态烘焙的，不应依赖构建时的相机位置：
        // tryExtractRenderState 内部的 shouldRender 会按 getViewDistance()（默认 64 格）做距离剔除，
        // 若区块在远处加载/重建（例如玩家在渲染距离外走动导致区块卸载重载），会得到 null，
        // 区域几何变空，且之后 addIfPossible 因 BE 已在集合中而不再触发重建，导致回来也不渲染。
        // 因此这里直接调用渲染器提取渲染状态，跳过距离剔除；
        // 距离剔除交给 renderInternal 在每帧绘制时按 renderDistance 判断。
        BlockEntityRenderDispatcher blockEntityDispatcher = Minecraft.getInstance().levelRenderer.blockEntityRenderDispatcher;
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().position();

        for (BlockEntity be : new ArrayList<>(cachedRenderingChunk.getBlockEntities())) {
            if (cancelled) {
                bufferSource.close();
                return;
            }
            if (be.isRemoved()) {
                continue;
            }

            poseStack.pushPose();
            BlockPos pos = be.getBlockPos();
            poseStack.translate(
                pos.getX() - cachedRenderingChunk.getChunkPos().getMinBlockX(),
                pos.getY(),
                pos.getZ() - cachedRenderingChunk.getChunkPos().getMinBlockZ()
            );

            SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
            BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = blockEntityDispatcher.getRenderer(be);
            BlockEntityRenderState renderState = null;
            if (renderer != null && be.hasLevel() && be.getType().isValid(be.getBlockState())) {
                renderState = renderer.createRenderState();
                renderer.extractRenderState(be, renderState, 0, cameraPosition, null);
            }

            if (renderState instanceof ItemBlockEntityRenderState itemBlockEntityRenderState) {
                List<SlotRenderState> renderData = itemBlockEntityRenderState.renderData;
                List<TransformItemData> transformData = itemBlockEntityRenderState.transformData;
                if (renderData.size() != transformData.size()) {
                    break;
                }
                for (int j = 0, renderDataSize = renderData.size(); j < renderDataSize; j++) {
                    TransformItemData transformDatum = transformData.get(j);
                    SlotRenderState slotRenderState = renderData.get(j);
                    if (slotRenderState.isBlock) {
                        BlockModelRenderState blockState = slotRenderState.blockState;
                        if (blockState == null) {
                            continue;
                        }
                        blockState.submit(
                            poseStack,
                            submitNodeStorage,
                            renderState.lightCoords,
                            OverlayTexture.NO_OVERLAY,
                            0
                        );
                    } else {
                        ItemStackRenderState itemRenderState = slotRenderState.itemState;
                        if (itemRenderState == null) {
                            continue;
                        }
                        int color = transformDatum.color;
                        for (ItemStackRenderState.LayerRenderState layer : itemRenderState.layers) {
                            if (layer.specialRenderer != null) {
                                poseStack.pushPose();
                                YuushyaUtils.scale(poseStack, transformDatum.scales);
                                YuushyaUtils.translate(poseStack, transformDatum.pos);
                                YuushyaUtils.rotate(poseStack, transformDatum.rot);
                                layer.submit(
                                    poseStack,
                                    submitNodeStorage,
                                    renderState.lightCoords,
                                    OverlayTexture.NO_OVERLAY,
                                    0
                                );
                                poseStack.popPose();
                            } else {
                                PoseStack poseStack2 = new PoseStack();
                                List<BakedQuad> bakedQuads = layer.prepareQuadList();
                                List<BakedQuad> newQuads = new ArrayList<>();
                                for (BakedQuad bakedQuad : bakedQuads) {
                                    poseStack2.pushPose();
                                    YuushyaUtils.scale(poseStack2, transformDatum.scales);
                                    YuushyaUtils.translate(poseStack2, transformDatum.pos);
                                    YuushyaUtils.rotate(poseStack2, transformDatum.rot);
                                    Vector3fc[] vector4fs = new Vector3fc[4];
                                    for (int i = 0; i < 4; i++) {
                                        Vector3fc position = bakedQuad.position(i);
                                        Vector4f vector4f = new Vector4f(
                                            position.x(),
                                            position.y(),
                                            position.z(),
                                            1
                                        );
                                        poseStack2.last().pose().transform(vector4f);
                                        vector4fs[i] = new Vector3f(vector4f.x(), vector4f.y(), vector4f.z());
                                    }
                                    poseStack2.popPose();

                                    BakedColors.PerQuad bakedColors = new BakedColors.PerQuad(color);
                                    newQuads.add(new BakedQuad(
                                        vector4fs[0],
                                        vector4fs[1],
                                        vector4fs[2],
                                        vector4fs[3],
                                        bakedQuad.packedUV0(),
                                        bakedQuad.packedUV1(),
                                        bakedQuad.packedUV2(),
                                        bakedQuad.packedUV3(),
                                        bakedQuad.direction(),
                                        bakedQuad.materialInfo(),
                                        bakedQuad.bakedNormals(),
                                        bakedColors
                                    ));
                                }

                                submitNodeStorage.submitItem(
                                    poseStack,
                                    ItemDisplayContext.NONE,
                                    renderState.lightCoords,
                                    OverlayTexture.NO_OVERLAY,
                                    0,
                                    EMPTY_TINTS,
                                    newQuads,
                                    ItemStackRenderState.FoilType.NONE
                                );
                            }
                        }
                    }
                }
            }

            FeatureRenderDispatcher dispatcher = getFeatureRenderDispatcher(submitNodeStorage, bufferSource);
            dispatcher.endFrame();

            poseStack.popPose();
        }

        cachedRenderingChunk.setEmpty(bufferSource.isEmpty());
        bufferSource.upload(cachedRenderingChunk);
        cachedRenderingChunk.replaceMeshData(bufferSource.getMeshSorts(), bufferSource.getIndexCountMap());
        cachedRenderingChunk.setLastRebuildTask(null);
    }

    private @NonNull FeatureRenderDispatcher getFeatureRenderDispatcher(
        SubmitNodeStorage submitNodeStorage,
        FullyBufferedBufferSource bufferSource
    ) {
        FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
            submitNodeStorage,
            Minecraft.getInstance().getModelManager(),
            bufferSource,
            Minecraft.getInstance().getAtlasManager(),
            CachedRenderingChunk.EmptyOutlineBufferSource.INSTANCE,
            CachedRenderingChunk.EmptyBufferSource.INSTANCE,
            Minecraft.getInstance().font,
            Minecraft.getInstance().gameRenderer.getGameRenderState()
        );

        dispatcher.renderAllFeatures();
        return dispatcher;
    }

    void cancel() {
        cancelled = true;
    }
}
