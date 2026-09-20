/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.DoubleArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
 *  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_124
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_8710
 */
package com.cobbleclub.server.world;

import com.cobbleclub.server.CobbleClubServer;
import com.cobbleclub.server.service.PermissionService;
import com.cobbleclub.server.service.RtpService;
import com.cobbleclub.server.world.ManagedBorderService;
import com.cobbleclub.server.world.ManagedWorldService;
import com.cobbleclub.server.world.WildPayloads;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.CustomPayload;

public final class ManagedWorldInitializer
implements ModInitializer {
    public void onInitialize() {
        DynamicDimensionLoadCallback.register(ManagedWorldService::loadPersistedDimensionsEarly);
        PayloadTypeRegistry.playS2C().register(WildPayloads.Open.ID, WildPayloads.Open.CODEC);
        PayloadTypeRegistry.playC2S().register(WildPayloads.Select.ID, WildPayloads.Select.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WildPayloads.Select.ID, (payload, context) -> {
            String target = ManagedWorldService.normalize(payload.world());
            if (target != null) {
                RtpService.start(context.player(), target);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"wild").requires(source -> PermissionService.has(source, "cobbleclub.command.wild", true))).executes(context -> ManagedWorldInitializer.openWild(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((LiteralArgumentBuilder)CommandManager.literal((String)"cobbleclubserver").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"world").requires(source -> PermissionService.admin(source, "cobbleclub.admin.world", 2))).then(CommandManager.literal((String)"list").executes(context -> {
                for (String world : ManagedWorldService.logicalNames()) {
                    ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal((String)ManagedWorldService.status(((ServerCommandSource)context.getSource()).getServer(), world)), false);
                }
                return ManagedWorldService.logicalNames().size();
            }))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)CommandManager.argument((String)"world", (ArgumentType)StringArgumentType.word()).executes(context -> {
                String name = StringArgumentType.getString((CommandContext)context, (String)"world");
                ((ServerCommandSource)context.getSource()).sendFeedback(() -> Text.literal((String)ManagedWorldService.status(((ServerCommandSource)context.getSource()).getServer(), name)), false);
                return ManagedWorldService.isManagedName(name) ? 1 : 0;
            })).then(((LiteralArgumentBuilder)CommandManager.literal((String)"regenerate").requires(source -> PermissionService.admin(source, "cobbleclub.admin.world.regenerate", 2))).executes(context -> ManagedWorldService.regenerate(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))).then(((LiteralArgumentBuilder)CommandManager.literal((String)"setspawn").requires(source -> PermissionService.admin(source, "cobbleclub.admin.world.setspawn", 2))).then(CommandManager.literal((String)"here").executes(context -> ManagedWorldService.setSpawnHere(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), ((ServerCommandSource)context.getSource()).getPlayer(), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))))));
            dispatcher.register((LiteralArgumentBuilder)CommandManager.literal((String)"cobbleclubserver").then(((LiteralArgumentBuilder)CommandManager.literal((String)"border").requires(source -> PermissionService.admin(source, "cobbleclub.admin.border", 2))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)CommandManager.argument((String)"world", (ArgumentType)StringArgumentType.word()).then(CommandManager.literal((String)"get").executes(context -> ManagedBorderService.get(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, false))))).then(CommandManager.literal((String)"set").then(CommandManager.argument((String)"size", (ArgumentType)DoubleArgumentType.doubleArg((double)2.0, (double)5.9999968E7)).executes(context -> ManagedBorderService.setSize(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), DoubleArgumentType.getDouble((CommandContext)context, (String)"size"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true)))))).then(((LiteralArgumentBuilder)CommandManager.literal((String)"center").then(CommandManager.literal((String)"spawn").executes(context -> ManagedBorderService.centerOnSpawn(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))).then(CommandManager.argument((String)"x", (ArgumentType)DoubleArgumentType.doubleArg()).then(CommandManager.argument((String)"z", (ArgumentType)DoubleArgumentType.doubleArg()).executes(context -> ManagedBorderService.setCenter(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), DoubleArgumentType.getDouble((CommandContext)context, (String)"x"), DoubleArgumentType.getDouble((CommandContext)context, (String)"z"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))))).then(CommandManager.literal((String)"on").executes(context -> ManagedBorderService.setEnabled(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), true, text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))).then(CommandManager.literal((String)"off").executes(context -> ManagedBorderService.setEnabled(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), false, text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true))))).then(CommandManager.literal((String)"reset").executes(context -> ManagedBorderService.reset(((ServerCommandSource)context.getSource()).getServer(), StringArgumentType.getString((CommandContext)context, (String)"world"), text -> ((ServerCommandSource)context.getSource()).sendFeedback(() -> text, true)))))));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ManagedWorldService.initialize(server);
            ManagedBorderService.initialize(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ManagedBorderService.shutdown();
            ManagedWorldService.shutdown(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ManagedBorderService.sync(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> ManagedBorderService.sync(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> ManagedBorderService.sync(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ManagedWorldService.tick(server);
            ManagedWorldService.runScheduled();
            ManagedBorderService.tick(server);
        });
    }

    private static int openWild(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend((ServerPlayerEntity)player, WildPayloads.Open.ID)) {
            player.sendMessage((Text)Text.literal((String)"The matching CobbleClub client mod is required to open /wild.").formatted(Formatting.RED), false);
            return 0;
        }
        List<String> worlds = ManagedWorldService.logicalNames();
        String subtitle = worlds.size() + " managed wild worlds";
        CobbleClubServer.LOGGER.info("[Wild v5] Sending {} managed worlds to {}: {}", new Object[]{worlds.size(), player.getName().getString(), worlds});
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)new WildPayloads.Open(subtitle, worlds));
        return 1;
    }
}

