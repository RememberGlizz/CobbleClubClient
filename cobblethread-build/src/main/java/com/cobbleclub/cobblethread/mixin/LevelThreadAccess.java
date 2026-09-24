package com.cobbleclub.cobblethread.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelThreadAccess {
    @Accessor("thread")
    Thread cobblethread$getThread();

    @Accessor("thread")
    void cobblethread$setThread(Thread thread);
}
