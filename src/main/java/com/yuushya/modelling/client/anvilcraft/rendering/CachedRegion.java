package com.yuushya.modelling.client.anvilcraft.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.client.ByteBufferBuilder;
import com.yuushya.modelling.client.NeoItemBlockModel;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.YuushyaDataTags;
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
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.IQuadTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL46;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.minecraftforge.client.model.QuadTransformers.toABGR;

/**
 * @author ZhuRuoLing
 */
public class CachedRegion {
    public static final RenderType TRANSLUCENT_MAIN = RenderType.create(
            "translucent_main", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true, translucentState(RENDERTYPE_TRANSLUCENT_SHADER)
    );
    private static final Direction[] DIRECTIONS = Direction.values();
    private final ChunkPos chunkPos;
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final CacheableBERenderingPipeline pipeline;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final RandomSource random = RandomSource.create();
    private Map<RenderType, VertexBuffer> buffers = new HashMap<>();
    private Map<RenderType, BufferBuilder.SortState> meshSortings = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    private ModelBlockRenderer.AmbientOcclusionFace aoFace = new ModelBlockRenderer.AmbientOcclusionFace();
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

    private static float @NotNull [] getColorComponents(TransformItemData transformData, BakedModel model, BakedQuad bakedQuad) {
        Color color = new Color(transformData.color);

        if (model instanceof NeoItemBlockModel) {
            int[] vertices = bakedQuad.getVertices();
            color = new Color(toABGR(vertices[IQuadTransformer.STRIDE + IQuadTransformer.COLOR]));
        }

        float[] colorComponents = new float[3];
        color.getColorComponents(colorComponents);
        return colorComponents;
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

    private void renderInternal(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Collection<RenderType> renderTypes
    ) {
        if (isEmpty) return;
        RenderSystem.enableBlend();
        Window window = Minecraft.getInstance().getWindow();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        if (cameraPosition.distanceTo(new Vec3(chunkPos.x * 16, cameraPosition.y, chunkPos.z * 16)) > renderDistance) {
            return;
        }
        List<RenderType> renderingOrders = new ArrayList<>(renderTypes);
        //TODO : 这里可能会因为排序影响性能
//        renderingOrders.sort(Comparator.comparingInt(a -> (a.sortOnUpload ? 1 : 0)));
        for (RenderType renderType : renderingOrders) {
            VertexBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            renderLayer(renderType, vb, frustumMatrix, projectionMatrix, cameraPosition, window);
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(VertexBuffer::close);
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

        // MC 1.20: drawWithShader 内部会处理 projection 和 modelView
        Uniform uniform = shader.CHUNK_OFFSET;

        if (uniform != null) {
            uniform.set(
                (float) (chunkPos.getMinBlockX() -cameraPosition.x),
                (float) (-cameraPosition.y),
                (float) (chunkPos.getMinBlockZ() -cameraPosition.z)
            );
        }

        vertexBuffer.bind();
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

    private void calculateShape(BlockAndTintGetter level, BlockState state, BlockPos pos, int[] vertices, Direction direction, @javax.annotation.Nullable float[] shape, BitSet shapeFlags) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; ++i) {
            float f6 = Float.intBitsToFloat(vertices[i * 8]);
            float f7 = Float.intBitsToFloat(vertices[i * 8 + 1]);
            float f8 = Float.intBitsToFloat(vertices[i * 8 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (shape != null) {
            shape[Direction.WEST.get3DDataValue()] = f;
            shape[Direction.EAST.get3DDataValue()] = f3;
            shape[Direction.DOWN.get3DDataValue()] = f1;
            shape[Direction.UP.get3DDataValue()] = f4;
            shape[Direction.NORTH.get3DDataValue()] = f2;
            shape[Direction.SOUTH.get3DDataValue()] = f5;
            int j = DIRECTIONS.length;
            shape[Direction.WEST.get3DDataValue() + j] = 1.0F - f;
            shape[Direction.EAST.get3DDataValue() + j] = 1.0F - f3;
            shape[Direction.DOWN.get3DDataValue() + j] = 1.0F - f1;
            shape[Direction.UP.get3DDataValue() + j] = 1.0F - f4;
            shape[Direction.NORTH.get3DDataValue() + j] = 1.0F - f2;
            shape[Direction.SOUTH.get3DDataValue() + j] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;
        switch (direction) {
            case DOWN:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f1 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case UP:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f4 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case NORTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f2 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case SOUTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f5 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case WEST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case EAST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f3 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
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
                if (be instanceof ItemBlockEntity itemBlockEntity) {
                    LocalPlayer localPlayer = mc.player;
                    if (be.getLevel() == null) {
                        bufferSource.close();
                        return;
                    }
                    ItemRenderer itemRenderer = mc.getItemRenderer();
                    BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
                    ArrayList<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
                    directions.add(null); // 加个null
                    float f = itemBlockEntity.getBlockState().getValue(HORIZONTAL_FACING).toYRot();
                    List<TransformItemData> transformDatas = itemBlockEntity.getTransformData();
                    Level level = be.getLevel();
                    BlockPos pos = be.getBlockPos();
                    Boolean disableAO = be.getBlockState().getValue(ENABLE_AO);

                    for (TransformItemData transformData : transformDatas)
                        if (transformData.isShown) {
                            ItemStack itemStack = transformData.itemStack;
                            if (itemStack.isEmpty()) {
                                continue;
                            }
                            //TODO 因为DefaultVertexFormat不对无法渲染文本，待处理，这里先剔除
                            if (itemStack.is(ItemRegistry.TEXT_BLOCK.get())) {
                                continue;
                            }
                            BakedModel blockModel;
                            BlockState blockState = YuushyaDataTags.getBlockState(itemStack);
                            if (transformData.enableBlock && blockState != null) {
                                blockModel = blockRenderer.getBlockModel(blockState);
                                for (Direction value : directions) {
                                    List<BakedQuad> blockModelQuads = blockModel.getQuads(blockState, value, random);
                                    float[] afloat = new float[DIRECTIONS.length * 2];
                                    BlockPos offset = pos.offset((int) (transformData.pos.x / 16), (int) (transformData.pos.y / 16), (int) (transformData.pos.z / 16));
                                    putNormalModel(poseStack, bufferSource, be, f, level, pos, disableAO, transformData, blockModel, blockModelQuads, afloat, offset);
                                }
                            } else {
                                blockModel = itemRenderer.getModel(itemStack, null, null, localPlayer.getId());
                                for (BakedModel model : blockModel.getRenderPasses(itemStack, true)) {
                                    BlockPos offset = pos.offset((int) (transformData.pos.x / 16), (int) (transformData.pos.y / 16), (int) (transformData.pos.z / 16));
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
                                        int packedLight = LevelRenderer.getLightColor(level, offset);
                                        itemRenderer.render(itemStack, ItemDisplayContext.NONE, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, model);
                                        poseStack.popPose();
                                    } else {
                                        for (Direction value : directions) {
                                            float[] afloat = new float[DIRECTIONS.length * 2];
                                            List<BakedQuad> blockModelQuads = model.getQuads(null, value, random);
                                            putNormalModel(poseStack, bufferSource, be, f, level, pos, disableAO, transformData, model, blockModelQuads, afloat, offset);
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
                    pipeline::submitUploadTask);

            CachedRegion.this.meshSortings = bufferSource.getMeshSorts();
            CachedRegion.this.indexCountMap = bufferSource.getIndexCountMap();
            lastRebuildTask = null;
        }

        private void putNormalModel(PoseStack poseStack, FullyBufferedBufferSource bufferSource, BlockEntity be, float f, Level level, BlockPos pos, Boolean disableAO, TransformItemData transformData, BakedModel blockModel, List<BakedQuad> blockModelQuads, float[] afloat, BlockPos offset) {
            for (BakedQuad bakedQuad : blockModelQuads) {
                poseStack.pushPose();
                {
                    poseStack.translate(
                        pos.getX() & 15,
                        pos.getY(),
                        pos.getZ() & 15
                    );

                    poseStack.translate(0.5f, 0.5f, 0.5f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-f));
                    poseStack.translate(-0.5f, -0.5f, -0.5f);

                    YuushyaUtils.scale(poseStack, transformData.scales);
                    YuushyaUtils.translate(poseStack, transformData.pos);
                    YuushyaUtils.rotate(poseStack, transformData.rot);

                    float[] colorComponents = getColorComponents(transformData, blockModel, bakedQuad);

                    if (disableAO) {
                        BitSet bitset = new BitSet(3);
                        calculateShape(level, be.getBlockState(), offset, bakedQuad.getVertices(), bakedQuad.getDirection(), afloat, bitset);
                        aoFace.calculate(level, be.getBlockState(), offset, bakedQuad.getDirection(), afloat, bitset, bakedQuad.isShade());
                        bufferSource.getBuffer(TRANSLUCENT_MAIN).putBulkData(poseStack.last(), bakedQuad, aoFace.brightness, colorComponents[0], colorComponents[1], colorComponents[2], 1.0f, aoFace.lightmap, OverlayTexture.NO_OVERLAY, true);
                    } else {
                        int packedLight = LevelRenderer.getLightColor(level, pos.offset((int) (transformData.pos.x / 16), (int) (transformData.pos.y / 16), (int) (transformData.pos.z / 16)));
                        bufferSource.getBuffer(TRANSLUCENT_MAIN).putBulkData(poseStack.last(), bakedQuad, colorComponents[0], colorComponents[1], colorComponents[2], packedLight, OverlayTexture.NO_OVERLAY);
                    }
                }
                poseStack.popPose();
            }
        }

        void cancel() {
            cancelled = true;
        }
    }
}
