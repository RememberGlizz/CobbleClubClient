/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2780$class_3976
 *  net.minecraft.class_2784
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.cobbleclub.server.mixin;

import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={WorldBorderListener.WorldBorderSyncer.class})
public interface WorldBorderSyncerAccessor {
    @Accessor(value="border")
    public WorldBorder cobbleclub$getBorder();
}

