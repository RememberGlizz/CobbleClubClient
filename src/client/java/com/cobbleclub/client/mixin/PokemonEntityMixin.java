package com.cobbleclub.client.mixin;

import com.cobbleclub.client.duck.PokemonEntityDuck;
import com.cobbleclub.client.state.TeraCrystalState;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonEntityMixin implements PokemonEntityDuck {
    @Unique
    private final TeraCrystalState cobbleclub$teraCrystalState =
            new TeraCrystalState();

    @Unique
    private boolean cobbleclub$teraCrystalPlayed = false;

    @Unique
    private boolean cobbleclub$teraCrystalPass = false;

    @Unique
    private double cobbleclub$animCrystalSeconds = 0.0;

    @Unique
    private long cobbleclub$lastCrystalTimeNs = -1L;

    @Override
    public TeraCrystalState cobbleclub$getTeraCrystalState() {
        return this.cobbleclub$teraCrystalState;
    }

    @Override
    public boolean cobbleclub$isTeraCrystalPlayed() {
        return this.cobbleclub$teraCrystalPlayed;
    }

    @Override
    public void cobbleclub$setTeraCrystalPlayed(boolean value) {
        this.cobbleclub$teraCrystalPlayed = value;
    }

    @Override
    public boolean cobbleclub$isTeraCrystalPass() {
        return this.cobbleclub$teraCrystalPass;
    }

    @Override
    public void cobbleclub$setTeraCrystalPass(boolean value) {
        this.cobbleclub$teraCrystalPass = value;
    }

    @Override
    public double cobbleclub$getAnimCrystalSeconds() {
        return this.cobbleclub$animCrystalSeconds;
    }

    @Override
    public void cobbleclub$setAnimCrystalSeconds(double value) {
        this.cobbleclub$animCrystalSeconds = value;
    }

    @Override
    public long cobbleclub$getLastCrystalTimeNs() {
        return this.cobbleclub$lastCrystalTimeNs;
    }

    @Override
    public void cobbleclub$setLastCrystalTimeNs(long value) {
        this.cobbleclub$lastCrystalTimeNs = value;
    }
}
