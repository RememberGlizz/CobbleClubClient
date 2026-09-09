package com.cobbleclub.server.service;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;

/** Registers CobbleClub's built-in player teleport request commands. */
public final class TpaModInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tpa")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))
                    .then(CommandManager.argument("player", StringArgumentType.word()).executes(context ->
                            TpaService.request(context.getSource().getPlayer(), StringArgumentType.getString(context, "player")))));

            dispatcher.register(CommandManager.literal("tpayes")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))
                    .executes(context -> TpaService.accept(context.getSource().getPlayer())));

            dispatcher.register(CommandManager.literal("tpano")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))
                    .executes(context -> TpaService.deny(context.getSource().getPlayer())));
        });

        ServerTickEvents.END_SERVER_TICK.register(TpaService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TpaService.forget(handler.player));
    }
}
