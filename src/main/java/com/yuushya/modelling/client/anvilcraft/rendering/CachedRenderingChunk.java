package com.yuushya.modelling.client.anvilcraft.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author ZhuRuoLing
 */
public class CachedRenderingChunk implements VertexBufferHost {
    @Getter
    private final ChunkPos chunkPos;
    private final Map<RenderType, GpuBuffer> buffers = new HashMap<>();
    private final Map<RenderType, GpuBuffer> indexBuffers = new HashMap<>();
    private final Map<RenderType, ByteBufferBuilder> sortBuffers = new HashMap<>();
    private final Minecraft minecraft = Minecraft.getInstance();
    @Getter
    private final Set<BlockEntity> blockEntities = new HashSet<>();

    private Map<RenderType, MeshData.SortState> meshSorting = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();

    private final CachedBlockEntityRenderingPipeline pipeline;

    @Nullable
    @Setter(AccessLevel.PACKAGE)
    private RebuildTask lastRebuildTask;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean isEmpty = true;

    private boolean isFreshMesh = true;


    public CachedRenderingChunk(ChunkPos chunkPos, CachedBlockEntityRenderingPipeline pipeline) {
        this.chunkPos = chunkPos;
        this.pipeline = pipeline;
    }

    /**
     * Updates the block entities collection and triggers a rebuild of the region.
     * <p>
     *
     * @param be The block entity to update.
     * @see CachedBlockEntityRenderingPipeline#update(BlockEntity)
     */
    public void update(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean shouldRecompile = blockEntities.removeIf(BlockEntity::isRemoved);
        if (be.isRemoved()) {
            shouldRecompile |= blockEntities.remove(be);
            if (shouldRecompile) {
                pipeline.submitCompileTask(new RebuildTask(this));
            }
            return;
        }
        shouldRecompile |= blockEntities.add(be);
        if (shouldRecompile) {
            pipeline.submitCompileTask(new RebuildTask(this));
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
     * @see CachedBlockEntityRenderingPipeline#blockRemoved(BlockEntity)
     */
    public void blockRemoved(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean removedAny = blockEntities.removeIf(BlockEntity::isRemoved) || blockEntities.remove(be);
        if (removedAny) {
            pipeline.submitCompileTask(new RebuildTask(this));
        }
    }

    public void render(boolean translucent) {
        renderInternal(buffers.keySet(), translucent);
    }

    public GpuBuffer getBuffer(Map<RenderType, GpuBuffer> buffers, RenderType renderType, long size, int usage) {
        if (buffers.containsKey(renderType)) {
            GpuBuffer buffer = buffers.get(renderType);
            if (buffer.size() < size) {
                buffer = RenderSystem.getDevice().createBuffer(
                    () -> pipeline.truncateName(renderType.toString()),
                    usage | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC,
                    size
                );
                GpuBuffer old = buffers.put(renderType, buffer);
                if (old != null) {
                    old.close();
                }
            }

            return buffers.get(renderType);
        }
        GpuBuffer vb = RenderSystem.getDevice().createBuffer(
            () -> pipeline.truncateName(renderType.toString()),
            usage | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC,
            size
        );
        buffers.put(renderType, vb);
        return vb;
    }

    public GpuBuffer getVertexBuffer(RenderType renderType, long size) {
        return getBuffer(this.buffers, renderType, size, GpuBuffer.USAGE_VERTEX);
    }

    public GpuBuffer getIndexBuffer(RenderType renderType, long size) {
        return getBuffer(this.indexBuffers, renderType, size, GpuBuffer.USAGE_INDEX);
    }

    private ByteBufferBuilder requestSortBuffer(RenderType renderType) {
        if (sortBuffers.containsKey(renderType)) {
            return sortBuffers.get(renderType);
        }
        ByteBufferBuilder builder = new ByteBufferBuilder(4096);
        sortBuffers.put(renderType, builder);
        return builder;
    }

    private void renderInternal(Collection<RenderType> renderTypes, boolean translucent) {
        if (isEmpty) return;

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().position();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        if (cameraPosition.distanceTo(new Vec3(
            chunkPos.x() * 16,
            cameraPosition.y,
            chunkPos.z() * 16
        )) > renderDistance) {
            return;
        }

        for (RenderType renderType : renderTypes) {
            GpuBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            if (renderType.sortOnUpload() != translucent) continue;
            renderLayer(renderType, vb, cameraPosition);
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(GpuBuffer::close);
        sortBuffers.values().forEach(ByteBufferBuilder::close);
    }

    private void renderLayer(
        RenderType renderType,
        GpuBuffer vertexBuffer,
        Vec3 cameraPosition
    ) {
        MeshData.SortState sortState = this.meshSorting.get(renderType);
        int indexCount = indexCountMap.getInt(renderType);

        if (indexCount <= 0) return;

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        Consumer<Matrix4fStack> modelViewModifier = renderType.state.layeringTransform.getModifier();

        modelViewStack.pushMatrix();

        if (modelViewModifier != null) {
            modelViewModifier.accept(modelViewStack);
        }

        modelViewStack.translate(
            -(float) cameraPosition.x + chunkPos.getMinBlockX(),
            -(float) cameraPosition.y,
            -(float) cameraPosition.z + chunkPos.getMinBlockZ()
        );

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector3f(),
            renderType.state.textureTransform.getMatrix()
        );
        Map<String, RenderSetup.TextureAndSampler> textures = renderType.state.getTextures();

        IndexGenerationResult indexGenerationResult = getIndexBuffer(renderType, cameraPosition, sortState, indexCount);

        RenderTarget renderTarget = renderType.state.outputTarget.getRenderTarget();
        GpuTextureView colorTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.useDepth ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView()) : null;

        //noinspection DataFlowIssue
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Immediate draw for " + renderType,
            colorTexture,
            OptionalInt.empty(),
            depthTexture,
            OptionalDouble.empty()
        )) {
            renderPass.setPipeline(renderType.state.pipeline);
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                renderPass.enableScissor(
                    scissorState.x(),
                    scissorState.y(),
                    scissorState.width(),
                    scissorState.height()
                );
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertexBuffer);

            for (Map.Entry<String, RenderSetup.TextureAndSampler> entry : textures.entrySet()) {
                renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
            }

            renderPass.setIndexBuffer(indexGenerationResult.indices, indexGenerationResult.indexType);
            renderPass.drawIndexed(0, 0, indexCount, 1);
        }

        modelViewStack.popMatrix();
    }

    public void replaceData(Collection<BlockPos> entityPos, ClientLevel clientLevel) {
        List<BlockEntity> blockEntities = entityPos.stream()
            .map(clientLevel::getBlockEntity)
            .filter(Objects::nonNull)
            .toList();
        this.blockEntities.clear();
        this.blockEntities.addAll(blockEntities);
        pipeline.submitCompileTask(new RebuildTask(this));
    }

    public void forcedUpdate() {
        pipeline.submitCompileTask(new RebuildTask(this));
    }

    public <E extends BlockEntity> void addIfPossible(E blockEntity) {
        if (!blockEntities.contains(blockEntity)) {
            blockEntities.add(blockEntity);
            pipeline.submitCompileTask(new RebuildTask(this));
        } else if (isEmpty && !blockEntity.isRemoved()) {
            // 已在集合中但区域当前为空：可能之前远处重建被距离剔除导致几何为空，
            // 需要重新编译以恢复渲染。由于构建已不再做距离剔除，这里会收敛（一次重建后 isEmpty=false）。
            pipeline.submitCompileTask(new RebuildTask(this));
        }
    }

    public void submitCompileTask() {
        pipeline.submitCompileTask(new RebuildTask(this));
    }

    public ByteBufferBuilder getSortingByteBufferBuilder(RenderType renderType) {
        if (sortBuffers.containsKey(renderType)) {
            return sortBuffers.get(renderType);
        }
        ByteBufferBuilder builder = new ByteBufferBuilder(4096);
        sortBuffers.put(renderType, builder);
        return builder;
    }

    private CachedRenderingChunk.IndexGenerationResult getIndexBuffer(
        RenderType renderType,
        Vec3 cameraPosition,
        MeshData.@Nullable SortState sortState,
        int indexCount
    ) {
        VertexFormat.IndexType indexType;
        GpuBuffer indices;
        if (sortState == null) {
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
            indices = autoIndices.getBuffer(indexCount);
            indexType = autoIndices.type();
        } else {
            if (isFreshMesh || CachedBlockEntityRenderingPipeline.isCameraMoved()) {
                if (isFreshMesh) {
                    isFreshMesh = false;
                }
                Vector3f relativePos = cameraPosition.toVector3f().sub(
                    chunkPos.getMinBlockX(),
                    0,
                    chunkPos.getMinBlockZ()
                );

                ByteBufferBuilder builder = this.getSortingByteBufferBuilder(renderType);
                ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(
                    builder,
                    ALRMeshSorting.byDistance(relativePos)
                );


                if (result != null) {
                    indices = getIndexBuffer(renderType, (long) sortState.indexType().bytes * indexCount);
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(indices.slice(), result.byteBuffer());
                    indexType = sortState.indexType();
                    result.close();
                    builder.clear();
                } else {
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
                    indices = autoIndices.getBuffer(indexCount);
                    indexType = autoIndices.type();
                }
            } else {
                if (indexBuffers.containsKey(renderType)) {
                    indices = getIndexBuffer(renderType, (long) sortState.indexType().bytes * indexCount);
                    indexType = sortState.indexType();
                } else {
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
                    indices = autoIndices.getBuffer(indexCount);
                    indexType = autoIndices.type();
                }

            }
        }
        return new IndexGenerationResult(indices, indexType);
    }

    @Override
    public void acceptUploadAction(Runnable runnable) {
        this.pipeline.submitUploadTask(runnable);
    }

    public void replaceMeshData(
        Map<RenderType, MeshData.SortState> meshSorts,
        Reference2IntMap<RenderType> indexCountMap
    ) {
        this.meshSorting = meshSorts;
        this.indexCountMap = indexCountMap;
        this.indexBuffers.forEach((_, buffers) -> buffers.close());
        this.indexBuffers.clear();
        this.isFreshMesh = true;
    }

    public static class EmptyOutlineBufferSource extends OutlineBufferSource {

        public static final EmptyOutlineBufferSource INSTANCE = new EmptyOutlineBufferSource();

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return EmptyVC.INSTANCE;
        }
    }

    public static class EmptyBufferSource extends MultiBufferSource.BufferSource {

        public static final EmptyBufferSource INSTANCE = new EmptyBufferSource();

        protected EmptyBufferSource() {
            super(null, null);
        }

        @Override
        public @NonNull VertexConsumer getBuffer(RenderType renderType) {
            return EmptyVC.INSTANCE;
        }

        @Override
        public void endBatch() {

        }

        @Override
        public void endBatch(RenderType type) {

        }

        @Override
        public void endLastBatch() {

        }
    }

    public static class EmptyVC implements VertexConsumer {

        public static final EmptyVC INSTANCE = new EmptyVC();

        @Override
        public @NonNull VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setLineWidth(float width) {
            return this;
        }
    }

    private record IndexGenerationResult(GpuBuffer indices, VertexFormat.IndexType indexType) {
    }
}