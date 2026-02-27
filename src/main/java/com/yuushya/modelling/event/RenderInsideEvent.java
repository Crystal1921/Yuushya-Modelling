package com.yuushya.modelling.event;

import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderInsideEvent {
    @SubscribeEvent
    public static void renderInside(RenderBlockScreenEffectEvent event) {
        Block block = event.getBlockState().getBlock();
        if (block instanceof AbstractTransformBlock) {
            event.setCanceled(true);
        }
    }
}
