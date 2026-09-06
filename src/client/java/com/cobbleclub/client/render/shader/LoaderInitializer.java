package com.cobbleclub.client.render.shader;

import com.cobbleclub.client.CobbleClubClient;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class LoaderInitializer {
   public static <T> T getImplInstance(Class<T> abstractClss, String... impls) {
      if (impls != null && impls.length != 0) {
         Class<?> clss = null;

         for(int i = 0; clss == null && i < impls.length; ++i) {
            try {
               clss = Class.forName(impls[i]);
            } catch (ClassNotFoundException var7) {
            }
         }

         if (clss == null) {
            CobbleClubClient.LOGGER.error("No Implementation of {} found with given paths {}", abstractClss, Arrays.toString(impls));
         } else if (abstractClss.isAssignableFrom(clss)) {
            try {
               Constructor<?> constructor = clss.getDeclaredConstructor();
               return abstractClss.cast(constructor.newInstance());
            } catch (NoSuchMethodException var5) {
               CobbleClubClient.LOGGER.error("Implementation of {} needs to provide an no arg constructor", clss);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
               CobbleClubClient.LOGGER.error(((ReflectiveOperationException)e).getMessage());
            }
         }

         throw new IllegalStateException("Couldn't create an instance of " + String.valueOf(abstractClss));
      } else {
         throw new IllegalStateException("Couldn't create an instance of " + String.valueOf(abstractClss) + ". No implementations provided!");
      }
   }
}
