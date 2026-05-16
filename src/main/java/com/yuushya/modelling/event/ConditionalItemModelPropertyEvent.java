package com.yuushya.modelling.event;

import com.yuushya.modelling.YuushyaNeoForge;
import com.yuushya.modelling.utils.PosTransConditional;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;

@EventBusSubscriber
public class ConditionalItemModelPropertyEvent {
    @SubscribeEvent
    public static void register(RegisterSelectItemModelPropertyEvent event) {
        event.register(YuushyaNeoForge.id("pos_trans"), PosTransConditional.TYPE);
    }
}
