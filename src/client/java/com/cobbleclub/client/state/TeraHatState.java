/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.scheduling.SchedulingTracker
 *  com.cobblemon.mod.common.client.render.models.blockbench.PosableState
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobbleclub.client.state;

import com.cobblemon.mod.common.api.scheduling.SchedulingTracker;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TeraHatState
extends PosableState {
    private final SchedulingTracker schedulingTracker;

    public TeraHatState() {
        this.setPose("idle");
        this.schedulingTracker = new SchedulingTracker();
    }

    @Nullable
    public Entity getEntity() {
        return null;
    }

    public void updatePartialTicks(float partialTicks) {
        this.setCurrentPartialTicks(partialTicks);
    }

    @NotNull
    public SchedulingTracker getSchedulingTracker() {
        return this.schedulingTracker;
    }
}

