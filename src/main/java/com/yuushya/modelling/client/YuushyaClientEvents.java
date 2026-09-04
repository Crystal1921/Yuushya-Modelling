package com.yuushya.modelling.client;

import com.yuushya.modelling.client.anvilcraft.rendering.CachedBERenderingPipeline;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(Dist.CLIENT)
public class YuushyaClientEvents {
    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        if (CachedBERenderingPipeline.getInstance() != null) {
            CachedBERenderingPipeline.getInstance().runTasks();
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        if (CachedBERenderingPipeline.getInstance() != null) {
            CachedBERenderingPipeline.getInstance().render(
//                event.getLevelRenderState().cameraRenderState.cullFrustum,
                false
            );
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        if (CachedBERenderingPipeline.getInstance() != null) {
            CachedBERenderingPipeline.getInstance().render(
//                event.getLevelRenderState().cameraRenderState.cullFrustum,
                true
            );
        }
    }
}
