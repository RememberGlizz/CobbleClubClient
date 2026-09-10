package com.cobbleclub.server.service;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;

/** Registers active economy commands and Cobblemon event rewards. */
public final class ActivityEconomyInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        ActivityEconomyService.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sell")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.sell", true))
                    .executes(context -> ActivityEconomyService.showSellInfo(context.getSource().getPlayer()))
                    .then(CommandManager.literal("hand").executes(context ->
                            ActivityEconomyService.sellHand(context.getSource().getPlayer())))
                    .then(CommandManager.literal("all").executes(context ->
                            ActivityEconomyService.sellAll(context.getSource().getPlayer())))
                    .then(CommandManager.literal("prices").executes(context ->
                            ActivityEconomyService.showPrices(context.getSource().getPlayer()))));

            dispatcher.register(CommandManager.literal("contracts")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.contracts", true))
                    .executes(context -> ActivityEconomyService.showContracts(context.getSource().getPlayer())));
            dispatcher.register(CommandManager.literal("contract")
                    .requires(source -> PermissionService.has(source, "cobbleclub.command.contracts", true))
                    .executes(context -> ActivityEconomyService.showContracts(context.getSource().getPlayer())));
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(ActivityEconomyService::onPokemonCaptured);
        CobblemonEvents.BATTLE_VICTORY.subscribe(ActivityEconomyService::onBattleVictory);

        ServerLifecycleEvents.SERVER_STARTED.register(ActivityEconomyService::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ActivityEconomyService.save());
        ServerTickEvents.END_SERVER_TICK.register(ActivityEconomyService::tick);
    }
}
