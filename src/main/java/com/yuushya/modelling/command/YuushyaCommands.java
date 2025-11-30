package com.yuushya.modelling.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class YuushyaCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        ReloadModelCommand.register(event.getDispatcher());
    }
}
