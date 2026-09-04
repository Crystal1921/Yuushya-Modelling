package com.yuushya.modelling.client;

import com.yuushya.modelling.client.anvilcraft.rendering.CachedBlockEntityRenderingPipeline;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(Dist.CLIENT)
public class YuushyaClientEvents {
    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().runTasks();
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
//                event.getLevelRenderState().cameraRenderState.cullFrustum,
                false
            );
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
//                event.getLevelRenderState().cameraRenderState.cullFrustum,
                true
            );
        }
    }
}
