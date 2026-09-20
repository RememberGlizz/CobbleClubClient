/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1937
 *  net.minecraft.class_2960
 *  net.minecraft.server.MinecraftServer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cobbleclub.server.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"de.z0rdak.yawp.data.region.RegionDataManager"}, remap=false)
public abstract class YawpRegionSaveMixin {
    @Inject(method={"saveLevelData"}, at={@At(value="HEAD")}, cancellable=true, require=0, remap=false)
    private static void cobbleclub$skipMissingDynamicLevelData(MinecraftServer server, World world, CallbackInfo ci) {
        if (world == null) {
            ci.cancel();
            return;
        }
        try {
            Class<?> manager = Class.forName("de.z0rdak.yawp.data.region.RegionDataManager");
            Field storageField = manager.getDeclaredField("dimRegionStorage");
            storageField.setAccessible(true);
            Object rawStorage = storageField.get(null);
            if (!(rawStorage instanceof Map)) {
                return;
            }
            Map storage = (Map)rawStorage;
            Identifier dimension = world.getRegistryKey().getValue();
            if (!storage.containsKey(dimension) || storage.get(dimension) == null) {
                ci.cancel();
            }
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            // empty catch block
        }
    }
}

