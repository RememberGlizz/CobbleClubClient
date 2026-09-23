package com.cobbleclub.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class SpawnVitalsHudMixin {
    private static final String COBBLECLUB_SPAWN_DIMENSION = "minecraft:overworld";

    private static boolean cobbleclub$isSpawnWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null
                && COBBLECLUB_SPAWN_DIMENSION.equals(
                        client.world.getRegistryKey().getValue().toString()
                );
    }

    // InGameHud#renderHealthBar in Minecraft 1.21.1.
    @Inject(
            method = "method_37298(Lnet/minecraft/class_332;Lnet/minecraft/class_1657;IIIIFIIIZ)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobbleclub$hideSpawnHealth(
            DrawContext context,
            PlayerEntity player,
            int x,
            int y,
            int lines,
            int regeneratingHeartIndex,
            float maxHealth,
            int lastHealth,
            int health,
            int absorption,
            boolean blinking,
            CallbackInfo ci
    ) {
        if (cobbleclub$isSpawnWorld()) {
            ci.cancel();
        }
    }

    // InGameHud#renderFood in Minecraft 1.21.1.
    @Inject(
            method = "method_58477(Lnet/minecraft/class_332;Lnet/minecraft/class_1657;II)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobbleclub$hideSpawnHunger(
            DrawContext context,
            PlayerEntity player,
            int top,
            int right,
            CallbackInfo ci
    ) {
        if (cobbleclub$isSpawnWorld()) {
            ci.cancel();
        }
    }
}
