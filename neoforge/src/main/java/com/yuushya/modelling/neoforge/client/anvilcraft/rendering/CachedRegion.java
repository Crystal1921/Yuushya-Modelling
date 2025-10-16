package com.yuushya.modelling.neoforge.client.anvilcraft.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.neoforge.client.NeoItemBlockModel;
import com.yuushya.modelling.utils.YuushyaUtils;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.neoforged.neoforge.client.model.QuadTransformers.toABGR;

/**
 * @author ZhuRuoLing
 */
public class CachedRegion {
    public static final RenderType TRANSLUCENT_MAIN = RenderType.create(
            "translucent_main", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 786432, true, true, translucentState(RENDERTYPE_TRANSLUCENT_SHADER)
    );
    private final ChunkPos chunkPos;
    private final Map<RenderType, ByteBufferBuilder> sortBuffers = new HashMap<>();
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final CacheableBERenderingPipeline pipeline;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final RandomSource random = RandomSource.create();
    private Map<RenderType, VertexBuffer> buffers = new HashMap<>();
    private Map<RenderType, MeshData.SortState> meshSortings = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Nullable
    private RebuildTask lastRebuildTask;
    private boolean isEmpty = true;

    public CachedRegion(ChunkPos chunkPos, CacheableBERenderingPipeline pipeline) {
        this.chunkPos = chunkPos;
        this.pipeline = pipeline;
    }

    private static RenderType.CompositeState translucentState(RenderStateShard.ShaderStateShard state) {
        return RenderType.CompositeState.builder()
                .setLightmapState(LIGHTMAP)
                .setShaderState(state)
                .setTextureState(BLOCK_SHEET_MIPPED)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(MAIN_TARGET)
                .createCompositeState(true);
    }

    /**
     * Updates the block entities collection and triggers a rebuild of the region.
     * <p>
     *
     * @param be The block entity to update.
     * @see CacheableBERenderingPipeline#update(BlockEntity)
     */
    public void update(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean shouldRecompile = blockEntities.removeIf(BlockEntity::isRemoved);
        if (be.isRemoved()) {
            shouldRecompile |= blockEntities.remove(be);
            if (shouldRecompile) {
                pipeline.submitCompileTask(new RebuildTask());
            }
            return;
        }
        shouldRecompile |= blockEntities.add(be);
        if (shouldRecompile) {
            pipeline.submitCompileTask(new RebuildTask());
        }
    }

    /**
     * Handles the removal of a block entity from the system and initiates a cache rebuild.
     * <p>
     * When a block entity is removed, this method is called to update the internal state of the system.
     * It cancels any ongoing rebuild tasks, removes the specified block entity from the collection,
     * cleans up any other removed block entities, and then submits a new rebuild task to the pipeline.
     *
     * @param be The block entity that has been removed.
     * @see CacheableBERenderingPipeline#blockRemoved(BlockEntity)
     */
    public void blockRemoved(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean removedAny = blockEntities.removeIf(BlockEntity::isRemoved) || blockEntities.remove(be);
        if (removedAny) {
            pipeline.submitCompileTask(new RebuildTask());
        }
    }

    public void render(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        renderInternal(frustumMatrix, projectionMatrix, buffers.keySet());
    }

    public VertexBuffer getBuffer(RenderType renderType) {
        if (buffers.containsKey(renderType)) {
            return buffers.get(renderType);
        }
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffers.put(renderType, vb);
        return vb;
    }

    private ByteBufferBuilder requestSortBuffer(RenderType renderType) {
        if (sortBuffers.containsKey(renderType)) {
            return sortBuffers.get(renderType);
        }
        ByteBufferBuilder builder = new ByteBufferBuilder(4096);
        sortBuffers.put(renderType, builder);
        return builder;
    }

    private void renderInternal(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Collection<RenderType> renderTypes) {
        if (isEmpty) return;
        RenderSystem.enableBlend();
        Window window = Minecraft.getInstance().getWindow();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        if (cameraPosition.distanceTo(new Vec3(chunkPos.x * 16, cameraPosition.y, chunkPos.z * 16)) > renderDistance) {
            return;
        }
        List<RenderType> renderingOrders = new ArrayList<>(renderTypes);
        renderingOrders.sort(Comparator.comparingInt(a -> (a.sortOnUpload ? 1 : 0)));
        for (RenderType renderType : renderingOrders) {
            VertexBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            renderLayer(renderType, vb, frustumMatrix, projectionMatrix, cameraPosition, window);
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(VertexBuffer::close);
        sortBuffers.values().forEach(ByteBufferBuilder::close);
    }

    private void renderLayer(
            RenderType renderType,
            VertexBuffer vertexBuffer,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Vec3 cameraPosition,
            Window window
    ) {
        int indexCount = indexCountMap.getInt(renderType);
        if (indexCount <= 0) return;
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, frustumMatrix, projectionMatrix, window);
        Uniform uniform = shader.CHUNK_OFFSET;
        if (uniform != null) {
            uniform.set(
                    (float) -cameraPosition.x,
                    (float) -cameraPosition.y,
                    (float) -cameraPosition.z);
        }
        vertexBuffer.bind();
        if (renderType.sortOnUpload) {
            MeshData.SortState sortState = this.meshSortings.get(renderType);
            if (sortState != null) {
                ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(
                        this.requestSortBuffer(renderType),
                        VertexSorting.byDistance(cameraPosition.toVector3f()));
                if (result != null) {
                    vertexBuffer.uploadIndexBuffer(result);
                }
            }
        }
        vertexBuffer.drawWithShader(frustumMatrix, projectionMatrix, shader);
        VertexBuffer.unbind();
        if (uniform != null) {
            uniform.set(0.0F, 0.0F, 0.0F);
        }
        renderType.clearRenderState();
    }

    public void replaceData(Collection<BlockPos> entityPos, ClientLevel clientLevel) {
        List<BlockEntity> blockEntities = entityPos.stream()
                .map(clientLevel::getBlockEntity)
                .filter(Objects::nonNull)
                .toList();
        this.blockEntities.clear();
        this.blockEntities.addAll(blockEntities);
        pipeline.submitCompileTask(new RebuildTask());
    }

    public void forcedUpdate() {
        pipeline.submitCompileTask(new RebuildTask());
    }

    public <E extends BlockEntity> void addIfPossible(E blockEntity) {
        if (!blockEntities.contains(blockEntity)) {
            blockEntities.add(blockEntity);
            pipeline.submitCompileTask(new RebuildTask());
        }
    }

    private class RebuildTask implements Runnable {
        private boolean cancelled = false;

        @Override
        public void run() {
            lastRebuildTask = this;
            int vertexSize = YuushyaUtils.vertexSize();
            PoseStack poseStack = new PoseStack();
            CachedRegion.this.isEmpty = true;
            FullyBufferedBufferSource bufferSource = new FullyBufferedBufferSource();
            Minecraft mc = Minecraft.getInstance();
            for (BlockEntity be : new ArrayList<>(blockEntities)) {
                if (be instanceof ItemBlockEntity itemBlockEntity && mc.player instanceof LocalPlayer localPlayer) {
                    if (be.getLevel() == null) {
                        bufferSource.close();
                        return;
                    }
                    ItemRenderer renderer = mc.getItemRenderer();
                    ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
                    directions.add(null); // 加个null
                    float f = itemBlockEntity.getBlockState().getValue(HORIZONTAL_FACING).toYRot();
                    List<TransformItemData> transformDatas = itemBlockEntity.getTransformData();
                    Level level = be.getLevel();
                    BlockPos pos = be.getBlockPos();

                    for (TransformItemData transformData : transformDatas)
                        if (transformData.isShown) {
                            ItemStack itemStack = transformData.itemStack;
                            BakedModel blockModel = renderer.getModel(itemStack, null, null, localPlayer.getId());
                            for (BakedModel model : blockModel.getRenderPasses(itemStack, true)) {
                                if (model instanceof BuiltInModel) {
                                    poseStack.pushPose();
                                    {
                                        poseStack.translate(
                                                pos.getX(),
                                                pos.getY(),
                                                pos.getZ()
                                        );
                                        YuushyaUtils.scale(poseStack, transformData.scales);
                                        YuushyaUtils.translate(poseStack, transformData.pos);
                                        YuushyaUtils.rotate(poseStack, transformData.rot);
                                        poseStack.translate(0.5f, 0.5f, 0.5f);
                                    }
                                    int packedLight = LevelRenderer.getLightColor(level, pos.offset((int) (transformData.pos.x / 16), (int) (transformData.pos.y / 16), (int) (transformData.pos.z / 16)));
                                    renderer.render(itemStack, ItemDisplayContext.NONE, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, model);
                                    poseStack.popPose();
                                } else {
                                    for (Direction value : directions) {
                                        List<BakedQuad> blockModelQuads = model.getQuads(null, value, random);
                                        for (BakedQuad bakedQuad : blockModelQuads) {
                                            poseStack.pushPose();
                                            {
                                                poseStack.translate(
                                                        pos.getX(),
                                                        pos.getY(),
                                                        pos.getZ()
                                                );
                                                YuushyaUtils.scale(poseStack, transformData.scales);
                                                YuushyaUtils.translate(poseStack, transformData.pos);
                                                YuushyaUtils.rotate(poseStack, transformData.rot);
                                                Color color = new Color(transformData.color);

                                                if (model instanceof NeoItemBlockModel) {
                                                    int[] vertices = bakedQuad.getVertices();
                                                    color = new Color(toABGR(vertices[IQuadTransformer.STRIDE + IQuadTransformer.COLOR]));
                                                }

                                                float[] colorComponents = new float[3];
                                                color.getColorComponents(colorComponents);
                                                int packedLight = LevelRenderer.getLightColor(level, pos.offset((int) (transformData.pos.x / 16), (int) (transformData.pos.y / 16), (int) (transformData.pos.z / 16)));
                                                bufferSource.getBuffer(TRANSLUCENT_MAIN).putBulkData(poseStack.last(), bakedQuad, colorComponents[0], colorComponents[1], colorComponents[2], 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
                                            }
                                            poseStack.popPose();
                                        }
                                    }
                                }
                            }
                        }
                }
            }
            CachedRegion.this.isEmpty = bufferSource.isEmpty();
            bufferSource.upload(
                    CachedRegion.this::getBuffer,
                    CachedRegion.this::requestSortBuffer,
                    pipeline::submitUploadTask);

            CachedRegion.this.meshSortings = bufferSource.getMeshSorts();
            CachedRegion.this.indexCountMap = bufferSource.getIndexCountMap();
            lastRebuildTask = null;
        }

        void cancel() {
            cancelled = true;
        }
    }
}
