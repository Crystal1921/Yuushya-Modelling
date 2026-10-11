package com.yuushya.modelling.event;

import com.yuushya.modelling.utils.CameraProjection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 世界渲染阶段的钩子：缓存投影矩阵，供变换编辑界面做拖拽换算。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientRenderEvent {

    @SubscribeEvent
    public static void captureProjection(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            CameraProjection.captureProjection(event.getProjectionMatrix());
        }
    }
}
