/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.fabric.api.event.player.AttackBlockCallback
 *  net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
 *  net.fabricmc.fabric.api.event.player.UseBlockCallback
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_1269
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2561
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 */
package com.cobbleclub.server.chestshop;

import com.cobbleclub.server.chestshop.ChestShopPayloads;
import com.cobbleclub.server.chestshop.ChestShopService;
import com.cobbleclub.server.service.PermissionService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.ActionResult;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ChestShopInitializer
implements ModInitializer {
    public void onInitialize() {
        ChestShopInitializer.registerPayloads();
        ChestShopInitializer.registerNetworking();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal((String)"chestshop").requires(source -> PermissionService.has(source, "cobbleclub.command.chestshop", true))).executes(context -> {
            ChestShopService.arm(((ServerCommandSource)context.getSource()).getPlayer());
            return 1;
        })).then(CommandManager.literal((String)"cancel").executes(context -> {
            ChestShopService.cancel(((ServerCommandSource)context.getSource()).getPlayer());
            return 1;
        }))));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
            return ChestShopService.handleUse(serverPlayer, hand, hit);
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            ServerWorld serverWorld;
            block5: {
                block4: {
                    if (!(world instanceof ServerWorld)) break block4;
                    serverWorld = (ServerWorld)world;
                    if (player instanceof ServerPlayerEntity) break block5;
                }
                return true;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
            boolean allowed = ChestShopService.canBreak(serverPlayer, serverWorld, pos);
            if (!allowed) {
                serverPlayer.sendMessage((Text)Text.literal((String)"Only players with the claim's Chest Shops permission can break or edit this shop."), true);
            }
            return allowed;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld)world;
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
                    ChestShopService.afterBreak(serverPlayer, serverWorld, pos);
                }
            }
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ServerWorld serverWorld;
            block5: {
                block4: {
                    if (!(world instanceof ServerWorld)) break block4;
                    serverWorld = (ServerWorld)world;
                    if (player instanceof ServerPlayerEntity) break block5;
                }
                return ActionResult.PASS;
            }
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
            if (ChestShopService.canBreak(serverPlayer, serverWorld, pos)) {
                return ActionResult.PASS;
            }
            serverPlayer.sendMessage((Text)Text.literal((String)"That player shop is protected by the claim's Chest Shops permission."), true);
            return ActionResult.FAIL;
        });
        ServerLifecycleEvents.SERVER_STARTED.register(ChestShopService::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChestShopService.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(ChestShopService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ChestShopService.disconnect(handler.player));
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(ChestShopPayloads.Placement.ID, ChestShopPayloads.Placement.CODEC);
        PayloadTypeRegistry.playS2C().register(ChestShopPayloads.SelectOpen.ID, ChestShopPayloads.SelectOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(ChestShopPayloads.PriceOpen.ID, ChestShopPayloads.PriceOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(ChestShopPayloads.ShopOpen.ID, ChestShopPayloads.ShopOpen.CODEC);
        PayloadTypeRegistry.playS2C().register(ChestShopPayloads.Close.ID, ChestShopPayloads.Close.CODEC);
        PayloadTypeRegistry.playC2S().register(ChestShopPayloads.SelectItem.ID, ChestShopPayloads.SelectItem.CODEC);
        PayloadTypeRegistry.playC2S().register(ChestShopPayloads.SetPrice.ID, ChestShopPayloads.SetPrice.CODEC);
        PayloadTypeRegistry.playC2S().register(ChestShopPayloads.ShopAction.ID, ChestShopPayloads.ShopAction.CODEC);
        PayloadTypeRegistry.playC2S().register(ChestShopPayloads.CancelSetup.ID, ChestShopPayloads.CancelSetup.CODEC);
    }

    private static void registerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(ChestShopPayloads.SelectItem.ID, (payload, context) -> ChestShopService.selectInventoryItem(context.player(), payload.slot()));
        ServerPlayNetworking.registerGlobalReceiver(ChestShopPayloads.SetPrice.ID, (payload, context) -> ChestShopService.setPendingPrice(context.player(), payload.price()));
        ServerPlayNetworking.registerGlobalReceiver(ChestShopPayloads.ShopAction.ID, (payload, context) -> ChestShopService.handleShopAction(context.player(), payload.shopId(), payload.action(), payload.quantity()));
        ServerPlayNetworking.registerGlobalReceiver(ChestShopPayloads.CancelSetup.ID, (payload, context) -> ChestShopService.cancelSetup(context.player(), true));
    }
}

