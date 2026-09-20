/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.service.PermissionService;
import com.cobbleclub.server.service.TpaService;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;

public final class TpaModInitializer
implements ModInitializer {
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"tpa").requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))).then(CommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).executes(context -> TpaService.request(((ServerCommandSource)context.getSource()).getPlayer(), StringArgumentType.getString((CommandContext)context, (String)"player")))));
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"tpayes").requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))).executes(context -> TpaService.accept(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"tpano").requires(source -> PermissionService.has(source, "cobbleclub.command.tpa", true))).executes(context -> TpaService.deny(((ServerCommandSource)context.getSource()).getPlayer())));
        });
        ServerTickEvents.END_SERVER_TICK.register(TpaService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TpaService.forget(handler.player));
    }
}

