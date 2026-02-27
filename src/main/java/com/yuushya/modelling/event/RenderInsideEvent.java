package com.yuushya.modelling.event;

import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class RenderInsideEvent {
    @SubscribeEvent
    public static void renderInside(RenderBlockScreenEffectEvent event) {
        Block block = event.getBlockState().getBlock();
        if (block instanceof AbstractTransformBlock) {
            event.setCanceled(true);
        }
    }
}
