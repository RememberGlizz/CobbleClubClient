package com.cobbleclub.client.chestshop;

import com.cobbleclub.server.chestshop.ChestShopPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public final class ChestShopNetworking implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChestShopPlacementRenderer.init();

        ClientPlayNetworking.registerGlobalReceiver(
                ChestShopPayloads.Placement.ID,
                (payload, context) ->
                        ChestShopPlacementRenderer.setActive(
                                payload.active(),
                                payload.message()
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ChestShopPayloads.SelectOpen.ID,
                (payload, context) ->
                        context.client().setScreen(
                                new ChestShopSelectScreen(
                                        payload.title()
                                )
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ChestShopPayloads.PriceOpen.ID,
                (payload, context) ->
                        context.client().setScreen(
                                new ChestShopPriceScreen(
                                        payload.item(),
                                        payload.minimumPrice(),
                                        payload.recommendedPrice(),
                                        payload.currencySymbol(),
                                        payload.title()
                                )
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ChestShopPayloads.ShopOpen.ID,
                (payload, context) -> {
                    Screen current =
                            MinecraftClient.getInstance().currentScreen;

                    if (current instanceof ChestShopScreen shop
                            && shop.matches(payload.shopId())) {
                        shop.apply(payload);
                    } else {
                        context.client().setScreen(
                                new ChestShopScreen(payload)
                        );
                    }
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ChestShopPayloads.Close.ID,
                (payload, context) -> {
                    ChestShopPlacementRenderer.setActive(false, "");
                    context.client().setScreen(null);
                }
        );
    }

    public static void selectItem(int slot) {
        if (ClientPlayNetworking.canSend(
                ChestShopPayloads.SelectItem.ID
        )) {
            ClientPlayNetworking.send(
                    new ChestShopPayloads.SelectItem(slot)
            );
        }
    }

    public static void setPrice(long price) {
        if (ClientPlayNetworking.canSend(
                ChestShopPayloads.SetPrice.ID
        )) {
            ClientPlayNetworking.send(
                    new ChestShopPayloads.SetPrice(price)
            );
        }
    }

    public static void action(
            String shopId,
            String action,
            int quantity
    ) {
        if (ClientPlayNetworking.canSend(
                ChestShopPayloads.ShopAction.ID
        )) {
            ClientPlayNetworking.send(
                    new ChestShopPayloads.ShopAction(
                            shopId,
                            action,
                            quantity
                    )
            );
        }
    }

    public static void cancelSetup() {
        if (ClientPlayNetworking.canSend(
                ChestShopPayloads.CancelSetup.ID
        )) {
            ClientPlayNetworking.send(
                    new ChestShopPayloads.CancelSetup()
            );
        }
    }
}
