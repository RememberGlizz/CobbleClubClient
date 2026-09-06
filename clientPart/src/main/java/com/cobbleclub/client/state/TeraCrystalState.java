package com.cobbleclub.client.state;

import com.cobblemon.mod.common.api.scheduling.SchedulingTracker;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import net.minecraft.class_1297;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeraCrystalState extends PosableState {
   private final SchedulingTracker schedulingTracker;

   public TeraCrystalState() {
      this.setPose("idle");
      this.schedulingTracker = new SchedulingTracker();
   }

   public @Nullable class_1297 getEntity() {
      return null;
   }

   public void updatePartialTicks(float partialTicks) {
      this.setCurrentPartialTicks(partialTicks);
   }

   public void resetAnimation() {
      this.reset();
      this.setCurrentPartialTicks(0.0F);
   }

   public @NotNull SchedulingTracker getSchedulingTracker() {
      return this.schedulingTracker;
   }
}
