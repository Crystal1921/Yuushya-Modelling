package com.yuushya.modelling.command;

import dev.architectury.event.events.common.CommandRegistrationEvent;

public class YuushyaCommands {
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, context, selection) -> {
            System.out.println(selection);
            ReloadModelCommand.register(dispatcher);
        });
    }
}
