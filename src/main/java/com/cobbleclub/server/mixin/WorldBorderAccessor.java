/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2780
 *  net.minecraft.class_2784
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.cobbleclub.server.mixin;

import java.util.List;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={WorldBorder.class})
public interface WorldBorderAccessor {
    @Accessor(value="listeners")
    public List<WorldBorderListener> cobbleclub$getListeners();
}

