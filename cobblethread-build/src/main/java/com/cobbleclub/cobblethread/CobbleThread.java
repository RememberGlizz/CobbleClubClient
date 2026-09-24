package com.cobbleclub.cobblethread;

import com.cobbleclub.cobblethread.config.CobbleThreadConfig;
import com.cobbleclub.cobblethread.runtime.RuntimeManager;
import com.cobbleclub.cobblethread.runtime.WorldScheduler;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobbleThread implements ModInitializer {
    public static final String MOD_ID = "cobblethread";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final CobbleThreadConfig CONFIG = CobbleThreadConfig.load();
    public static final RuntimeManager RUNTIME = new RuntimeManager(CONFIG);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(RUNTIME::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(RUNTIME::stop);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        LOGGER.info("CobbleThread {} initialized in {} mode", CONFIG.enabled() ? "is" : "is not", CONFIG.strictOwnership() ? "strict" : "compatibility");
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext registryAccess,
                                  Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("cobblethread")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status").executes(context -> {
                WorldScheduler scheduler = RUNTIME.scheduler(context.getSource().getServer());
                String status = scheduler == null
                    ? "CobbleThread inactive"
                    : String.format(java.util.Locale.ROOT,
                        "CobbleThread active: %d workers, %d owned worlds, tick %d, queue %d, transfers %d/%d, world avg %.2f ms max %.2f ms",
                        scheduler.workerCount(), scheduler.assignmentCount(), scheduler.completedTicks(),
                        scheduler.compatibilityQueueDepth(), scheduler.completedTransfers(), scheduler.deferredTransfers(),
                        scheduler.averageWorldTickMillis(), scheduler.maxWorldTickMillis());
                context.getSource().sendSuccess(() -> Component.literal(status), false);
                return 1;
            })));
    }
}
