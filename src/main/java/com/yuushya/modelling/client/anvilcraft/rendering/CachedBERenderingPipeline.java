package com.yuushya.modelling.client.anvilcraft.rendering;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author ZhuRuoLing
 */
@EventBusSubscriber(Dist.CLIENT)
public class CachedBERenderingPipeline {
    @Nullable
    private static CachedBERenderingPipeline instance;
    final ClientLevel level;
    private final Queue<Runnable> pendingCompiles = new ArrayDeque<>();
    private final Queue<Runnable> pendingUploads = new ArrayDeque<>();
    private final Set<ChunkPos> dirtyChunks = new HashSet<>();
    private final Map<ChunkPos, CachedRegion> regions = new HashMap<>();
    private boolean valid = true;
    private static Vec3 cameraOldPosition = null;
    @Getter
    private static boolean cameraMoved = true;
    private static int glMaxLabelLength = 0;

    public static void create() {
        if (GL.getCapabilities().GL_KHR_debug) {
            glMaxLabelLength = GL46.glGetInteger(GL46.GL_MAX_LABEL_LENGTH);
            glMaxLabelLength /= 2;
        }
    }

    public CachedRegion getRenderRegion(ChunkPos chunkPos) {
        if (regions.containsKey(chunkPos)) {
            return regions.get(chunkPos);
        }
        CachedRegion region = new CachedRegion(chunkPos, this);
        regions.put(chunkPos, region);
        return region;
    }

    public CachedBERenderingPipeline(ClientLevel level) {
        this.level = level;
    }

    public void runTasks() {
        if (dirtyChunks.isEmpty()) return;
        var chunksToRebuild = List.copyOf(dirtyChunks);
        dirtyChunks.clear();

        for (ChunkPos chunkPos : chunksToRebuild) {
            CachedRegion chunk = regions.get(chunkPos);
            if (chunk != null) {
                chunk.submitCompileTask();
            }
        }

        while (!pendingCompiles.isEmpty() && valid) {
            pendingCompiles.poll().run();
        }
        while (!pendingUploads.isEmpty() && valid) {
            pendingUploads.poll().run();
        }
    }

    /**
     * Updates the rendering pipeline instance with a new level context.
     *
     * @param level The new ClientLevel instance that the rendering pipeline should be updated to use.
     */
    public static void updateLevel(ClientLevel level) {
        if (instance != null) {
            instance.releaseBuffers();
        }
        instance = new CachedBERenderingPipeline(level);
    }

    /**
     * Notifies the pipeline that a {@link BlockEntity} has been removed.
     * This method will be automatically called when a {@link BlockEntity} has been removed.
     *
     * @param be The removed {@link BlockEntity}
     */
    public void blockRemoved(BlockEntity be) {
        IBlockEntityRendererExtension<?> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher()
            .getRenderer(be);
        if (renderer == null) return;
        ChunkPos chunkPos = ChunkPos.containing(be.getBlockPos());
        getRenderRegion(chunkPos).blockRemoved(be);
    }

    public void updateFromNetwork(ChunkPos chunkPos, Collection<BlockPos> entityPos) {
        getRenderRegion(chunkPos).replaceData(entityPos, level);
    }

    /**
     * Notifies the pipeline that a {@link BlockEntity} has been updated and the cache should be rebuilt.
     *
     * @param be The updated {@link BlockEntity}
     */
    public void update(BlockEntity be) {
        BlockEntityRenderer<?, ?> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher()
            .getRenderer(be);
        if (renderer == null) return;
        ChunkPos chunkPos = ChunkPos.containing(be.getBlockPos());
        getRenderRegion(chunkPos).update(be);
    }

    public void submitUploadTask(Runnable task) {
        pendingUploads.add(task);
    }

    public void submitCompileTask(Runnable task) {
        pendingCompiles.add(task);
    }

    /**
     * Releases all buffers in use and mark current pipeline instance as invalid.
     */
    public void releaseBuffers() {
        regions.values().forEach(CachedRegion::releaseBuffers);
        valid = false;
    }

    public void render(boolean translucent) {
        for (CachedRegion value : regions.values()) {
            value.render(translucent);
        }
    }

    public String truncateName(String s) {
        return StringUtil.truncateStringIfNecessary(s, glMaxLabelLength, true);
    }

    /**
     * Retrieves the current instance of the CacheableBERenderingPipeline.
     *
     * @return The current instance of the CacheableBERenderingPipeline,
     * or null if there has no {@link ClientLevel} in current {@link Minecraft} client.
     */
    @Nullable
    public static CachedBERenderingPipeline getInstance() {
        return instance;
    }

    public void forcedUpdate(BlockPos pos) {
        getRenderRegion(ChunkPos.containing(pos)).forcedUpdate();
    }

    public void forcedUpdate() {
        for (CachedRegion value : this.regions.values()) {
            value.forcedUpdate();
        }
    }

    @ApiStatus.Internal
    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterSky event) {
        Vec3 pos = event.getLevelRenderState().cameraRenderState.pos;
        if (pos.equals(cameraOldPosition)) {
            cameraMoved = false;
            return;
        }
        cameraOldPosition = new Vec3(pos.x, pos.y, pos.z);
        cameraMoved = true;
    }

    @ApiStatus.Internal
    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        if (instance != null) {
            instance.handleIntegration();
        }
    }

    @SubscribeEvent
    public static void onSectionGeometry(AddSectionGeometryEvent event) {
        if (instance == null) return;
        instance.sectionRebuilt(event.getLevel(), event.getSectionOrigin());
    }

    private void sectionRebuilt(Level eventLevel, BlockPos sectionOrigin) {
        if (eventLevel != level) return;
        var chunk = level.getChunkAt(sectionOrigin);
        synchronized (dirtyChunks) {
            dirtyChunks.add(chunk.getPos());
        }
    }

    private void handleIntegration() {
        // intentionally empty
    }
}
