/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.render.shader;

import com.cobbleclub.client.CobbleClubClient;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class LoaderInitializer {
    public static <T> T getImplInstance(Class<T> abstractClss, String ... impls) {
        if (impls != null && impls.length != 0) {
            Class<?> clss = null;
            for (int i = 0; clss == null && i < impls.length; ++i) {
                try {
                    clss = Class.forName(impls[i]);
                    continue;
                }
                catch (ClassNotFoundException classNotFoundException) {
                    // empty catch block
                }
            }
            if (clss == null) {
                CobbleClubClient.LOGGER.error("No Implementation of {} found with given paths {}", abstractClss, (Object)Arrays.toString(impls));
            } else if (abstractClss.isAssignableFrom(clss)) {
                try {
                    Constructor<?> constructor = clss.getDeclaredConstructor(new Class[0]);
                    return abstractClss.cast(constructor.newInstance(new Object[0]));
                }
                catch (NoSuchMethodException var5) {
                    CobbleClubClient.LOGGER.error("Implementation of {} needs to provide an no arg constructor", clss);
                }
                catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    CobbleClubClient.LOGGER.error(e.getMessage());
                }
            }
            throw new IllegalStateException("Couldn't create an instance of " + String.valueOf(abstractClss));
        }
        throw new IllegalStateException("Couldn't create an instance of " + String.valueOf(abstractClss) + ". No implementations provided!");
    }
}

