package com.yuushya.modelling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yuushya.modelling.network.YuushyaModellingNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ReloadModelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("yuushya_reload_model")
                .then(Commands.literal("reload")
                        .then(Commands.literal("blocks")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return 0;
                                    }
                                    YuushyaModellingNetwork.INSTANCE.sendToClient(new ReloadModelPacket(ReloadType.BLOCKS), player.connection.getConnection(), net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                                    player.sendSystemMessage(Component.translatable("command.yuushya_modelling.reload_succeed"));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("items")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return 0;
                                    }
                                    YuushyaModellingNetwork.INSTANCE.sendToClient(new ReloadModelPacket(ReloadType.ITEMS), player.connection.getConnection(), net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                                    player.sendSystemMessage(Component.translatable("command.yuushya_modelling.reload_succeed"));
                                    return 1;
                                }))
                        .then(Commands.literal("texts")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return 0;
                                    }
                                    YuushyaModellingNetwork.INSTANCE.sendToClient(new ReloadModelPacket(ReloadType.TEXTS), player.connection.getConnection(), net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                                    player.sendSystemMessage(Component.translatable("command.yuushya_modelling.reload_succeed"));
                                    return 1;
                                }))
                        .then(Commands.literal("all")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return 0;
                                    }
                                    YuushyaModellingNetwork.INSTANCE.sendToClient(new ReloadModelPacket(ReloadType.ALL), player.connection.getConnection(), net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                                    player.sendSystemMessage(Component.translatable("command.yuushya_modelling.reload_succeed"));
                                    return 1;
                                }))
                ));
    }

    public enum ReloadType {
        BLOCKS,
        ITEMS,
        TEXTS,
        ALL
    }
}
