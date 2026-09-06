package com.cobbleclub.client.mixin;

import com.cobbleclub.client.duck.PokemonEntityDuck;
import com.cobbleclub.client.state.TeraCrystalState;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
   value = {PokemonEntity.class},
   remap = false
)
public abstract class PokemonEntityMixin implements PokemonEntityDuck {
   @Unique
   private final TeraCrystalState cobbleclub$teraCrystalState = new TeraCrystalState();
   @Unique
   private boolean cobbleclub$teraCrystalPlayed = false;
   @Unique
   private boolean cobbleclub$teraCrystalPass = false;
   @Unique
   private double cobbleclub$animCrystalSeconds = (double)0.0F;
   @Unique
   private long cobbleclub$lastCrystalTimeNs = -1L;

   public TeraCrystalState cobbleclub$getTeraCrystalState() {
      return this.cobbleclub$teraCrystalState;
   }

   public boolean cobbleclub$isTeraCrystalPlayed() {
      return this.cobbleclub$teraCrystalPlayed;
   }

   public void cobbleclub$setTeraCrystalPlayed(boolean value) {
      this.cobbleclub$teraCrystalPlayed = value;
   }

   public boolean cobbleclub$isTeraCrystalPass() {
      return this.cobbleclub$teraCrystalPass;
   }

   public void cobbleclub$setTeraCrystalPass(boolean value) {
      this.cobbleclub$teraCrystalPass = value;
   }

   public double cobbleclub$getAnimCrystalSeconds() {
      return this.cobbleclub$animCrystalSeconds;
   }

   public void cobbleclub$setAnimCrystalSeconds(double value) {
      this.cobbleclub$animCrystalSeconds = value;
   }

   public long cobbleclub$getLastCrystalTimeNs() {
      return this.cobbleclub$lastCrystalTimeNs;
   }

   public void cobbleclub$setLastCrystalTimeNs(long value) {
      this.cobbleclub$lastCrystalTimeNs = value;
   }
}
