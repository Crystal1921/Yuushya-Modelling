package com.yuushya.modelling.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class YuushyaCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        ReloadModelCommand.register(event.getDispatcher());
    }
}
