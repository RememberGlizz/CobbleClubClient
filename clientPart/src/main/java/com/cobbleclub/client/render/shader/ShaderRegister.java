package com.cobbleclub.client.render.shader;

import java.io.IOException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_5944;

@Environment(EnvType.CLIENT)
public interface ShaderRegister {
   Factory INSTANCE = (Factory)LoaderInitializer.getImplInstance(Factory.class, "com.cobbleclub.client.render.shader.ShaderRegisterFactory");

   default class_5944 create(class_2960 location, class_293 format) throws IOException {
      return this.create(location, format, true);
   }

   class_5944 create(class_2960 var1, class_293 var2, boolean var3) throws IOException;

   default void register(class_2960 location, class_293 format, Consumer<class_5944> loadCallback) throws IOException {
      this.register(this.create(location, format), loadCallback);
   }

   void register(class_5944 var1, Consumer<class_5944> var2);

   @Environment(EnvType.CLIENT)
   public interface Factory {
      void register(String var1, Consumer<ShaderRegister> var2);
   }
}
