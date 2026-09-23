/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.events.CobblemonEvents
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.network.ContractsPayloads;
import com.cobbleclub.server.service.ActivityEconomyService;
import com.cobbleclub.server.service.LeaderboardService;
import com.cobbleclub.server.service.PermissionService;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;

public final class ActivityEconomyInitializer
implements ModInitializer {
    public void onInitialize() {
        ActivityEconomyService.initialize();
        PayloadTypeRegistry.playC2S().register(ContractsPayloads.Action.ID, ContractsPayloads.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(ContractsPayloads.Open.ID, ContractsPayloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(ContractsPayloads.State.ID, ContractsPayloads.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                ContractsPayloads.Action.ID,
                (payload, context) -> context.server().execute(
                        () -> ContractsService.handleAction(context.player(), payload.json())
                )
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"sell").requires(source -> PermissionService.has(source, "cobbleclub.command.sell", true))).executes(context -> ActivityEconomyService.showSellInfo(((ServerCommandSource)context.getSource()).getPlayer()))).then(CommandManager.literal((String)"hand").executes(context -> ActivityEconomyService.sellHand(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.literal((String)"all").executes(context -> ActivityEconomyService.sellAll(((ServerCommandSource)context.getSource()).getPlayer())))).then(CommandManager.literal((String)"prices").executes(context -> ActivityEconomyService.showPrices(((ServerCommandSource)context.getSource()).getPlayer()))));
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"contracts").requires(source -> PermissionService.has(source, "cobbleclub.command.contracts", true))).executes(context -> ActivityEconomyService.showContracts(((ServerCommandSource)context.getSource()).getPlayer())));
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"contract").requires(source -> PermissionService.has(source, "cobbleclub.command.contracts", true))).executes(context -> ActivityEconomyService.showContracts(((ServerCommandSource)context.getSource()).getPlayer())));
        });
        CobblemonEvents.POKEMON_CAPTURED.subscribe(ActivityEconomyService::onPokemonCaptured);
        CobblemonEvents.BATTLE_VICTORY.subscribe(ActivityEconomyService::onBattleVictory);
        CobblemonEvents.POKEMON_CAPTURED.subscribe(LeaderboardService::onPokemonCaptured);
        CobblemonEvents.BATTLE_FAINTED.subscribe(LeaderboardService::onBattleFainted);
        ServerLifecycleEvents.SERVER_STARTED.register(ActivityEconomyService::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ActivityEconomyService.save());
        ServerTickEvents.END_SERVER_TICK.register(ActivityEconomyService::tick);
    }
}

