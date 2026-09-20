/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.duck;

import com.cobbleclub.client.state.TeraCrystalState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface PokemonEntityDuck {
    public boolean cobbleclub$isTeraCrystalPlayed();

    public void cobbleclub$setTeraCrystalPlayed(boolean var1);

    public boolean cobbleclub$isTeraCrystalPass();

    public void cobbleclub$setTeraCrystalPass(boolean var1);

    public double cobbleclub$getAnimCrystalSeconds();

    public void cobbleclub$setAnimCrystalSeconds(double var1);

    public long cobbleclub$getLastCrystalTimeNs();

    public void cobbleclub$setLastCrystalTimeNs(long var1);

    public TeraCrystalState cobbleclub$getTeraCrystalState();
}

