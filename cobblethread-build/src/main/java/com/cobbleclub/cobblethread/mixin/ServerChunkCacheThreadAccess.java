package com.cobbleclub.cobblethread.mixin;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheThreadAccess {
    @Accessor("mainThread")
    Thread cobblethread$getMainThread();

    @Accessor("mainThread")
    void cobblethread$setMainThread(Thread thread);
}
