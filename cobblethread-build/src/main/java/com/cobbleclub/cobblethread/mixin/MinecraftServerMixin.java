package com.cobbleclub.cobblethread.mixin;

import com.cobbleclub.cobblethread.CobbleThread;
import com.cobbleclub.cobblethread.runtime.WorldScheduler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class, priority = 1100)
public abstract class MinecraftServerMixin {
    @Shadow private int tickCount;
    @Shadow private PlayerList playerList;
    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    /**
     * Schedule the owned-world tick immediately before vanilla asks for the
     * world iterable. Keeping this hook on getAllLevels() is less sensitive to
     * local-variable/iterator layout changes than targeting Iterator.hasNext().
     */
    @Inject(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;",
        shift = At.Shift.BEFORE))
    private void cobblethread$tickOwnedWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        WorldScheduler scheduler = CobbleThread.RUNTIME.scheduler(server);
        if (scheduler != null) {
            scheduler.tickWorlds(getAllLevels(), shouldKeepTicking, tickCount, playerList);
        }
    }

    /**
     * Once CobbleThread has ticked the worlds behind its barrier, make the
     * original world loop empty. The rest of tickChildren (network, players,
     * GUI tickables, and other global server work) continues on the server
     * thread exactly where vanilla expects it.
     */
    @WrapOperation(method = "tickChildren", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"))
    private Iterable<ServerLevel> cobblethread$skipVanillaWorldLoop(
        MinecraftServer instance, Operation<Iterable<ServerLevel>> original) {
        return CobbleThread.RUNTIME.active(instance) ? List.of() : original.call(instance);
    }
}
