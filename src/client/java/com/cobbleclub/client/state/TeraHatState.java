package com.cobbleclub.client.state;

import com.cobblemon.mod.common.api.scheduling.SchedulingTracker;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeraHatState extends PosableState {
   private final SchedulingTracker schedulingTracker;

   public TeraHatState() {
      this.setPose("idle");
      this.schedulingTracker = new SchedulingTracker();
   }

   public @Nullable Entity getEntity() {
      return null;
   }

   public void updatePartialTicks(float partialTicks) {
      this.setCurrentPartialTicks(partialTicks);
   }

   public @NotNull SchedulingTracker getSchedulingTracker() {
      return this.schedulingTracker;
   }
}
