/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_2784
 *  net.minecraft.class_310
 */
package com.cobbleclub.client.world;

import com.cobbleclub.server.network.Payloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.client.MinecraftClient;

@Environment(value=EnvType.CLIENT)
public final class ManagedBorderClient {
    private static final double MAX_SIZE = 5.9999968E7;
    private static final double VISUAL_RANGE = 512.0;
    private static final int POST_SYNC_RETRY_TICKS = 20;
    private static BorderState state;
    private static AppliedState applied;
    private static int retryTicks;

    private ManagedBorderClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ManagedBorderState.ID, (payload, context) -> context.client().execute(() -> {
            state = new BorderState(payload.worldId(), payload.managed(), payload.enabled(), payload.centerX(), payload.centerZ(), payload.size());
            applied = null;
            retryTicks = 20;
            ManagedBorderClient.updateVisual(context.client(), true);
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean forceCheck;
            if (client == null || client.world == null || state == null) {
                return;
            }
            boolean bl = forceCheck = retryTicks > 0;
            if (retryTicks > 0) {
                --retryTicks;
            }
            ManagedBorderClient.updateVisual(client, forceCheck);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            state = null;
            applied = null;
            retryTicks = 0;
        });
    }

    private static void updateVisual(MinecraftClient client, boolean forceCheck) {
        boolean mismatch;
        AppliedState desired;
        BorderState current = state;
        if (client == null || client.world == null || current == null) {
            return;
        }
        String actualWorld = client.world.getRegistryKey().getValue().toString();
        if (!actualWorld.equals(current.worldId)) {
            return;
        }
        if (!current.managed) {
            applied = null;
            return;
        }
        boolean showRealBorder = current.enabled && ManagedBorderClient.isNearEdge(client, current);
        AppliedState appliedState = desired = showRealBorder ? new AppliedState(actualWorld, true, current.centerX, current.centerZ, current.size) : new AppliedState(actualWorld, false, 0.0, 0.0, 5.9999968E7);
        if (!forceCheck && desired.equals(applied)) {
            return;
        }
        WorldBorder border = client.world.getWorldBorder();
        boolean bl = mismatch = Math.abs(border.getCenterX() - desired.centerX) > 1.0E-4 || Math.abs(border.getCenterZ() - desired.centerZ) > 1.0E-4 || Math.abs(border.getSize() - desired.size) > 1.0E-4;
        if (mismatch) {
            if (Math.abs(border.getCenterX() - desired.centerX) > 1.0E-4 || Math.abs(border.getCenterZ() - desired.centerZ) > 1.0E-4) {
                border.setCenter(desired.centerX, desired.centerZ);
            }
            if (Math.abs(border.getSize() - desired.size) > 1.0E-4) {
                border.setSize(desired.size);
            }
        }
        applied = desired;
    }

    private static boolean isNearEdge(MinecraftClient client, BorderState border) {
        double dz;
        if (client.player == null || border.size <= 0.0) {
            return false;
        }
        double half = border.size / 2.0;
        double dx = Math.abs(client.player.getX() - border.centerX);
        double distanceToEdge = Math.min(half - dx, half - (dz = Math.abs(client.player.getZ() - border.centerZ)));
        return distanceToEdge <= 512.0;
    }

    @Environment(value=EnvType.CLIENT)
    private record BorderState(String worldId, boolean managed, boolean enabled, double centerX, double centerZ, double size) {
    }

    @Environment(value=EnvType.CLIENT)
    private record AppliedState(String worldId, boolean visible, double centerX, double centerZ, double size) {
    }
}

