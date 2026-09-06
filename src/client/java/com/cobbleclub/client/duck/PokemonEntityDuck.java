package com.cobbleclub.client.duck;

import com.cobbleclub.client.state.TeraCrystalState;

public interface PokemonEntityDuck {
   boolean cobbleclub$isTeraCrystalPlayed();

   void cobbleclub$setTeraCrystalPlayed(boolean var1);

   boolean cobbleclub$isTeraCrystalPass();

   void cobbleclub$setTeraCrystalPass(boolean var1);

   double cobbleclub$getAnimCrystalSeconds();

   void cobbleclub$setAnimCrystalSeconds(double var1);

   long cobbleclub$getLastCrystalTimeNs();

   void cobbleclub$setLastCrystalTimeNs(long var1);

   TeraCrystalState cobbleclub$getTeraCrystalState();
}
