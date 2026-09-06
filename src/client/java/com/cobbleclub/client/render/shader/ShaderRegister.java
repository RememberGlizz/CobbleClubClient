package com.cobbleclub.client.render.shader;

import java.io.IOException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public interface ShaderRegister {
   Factory INSTANCE = (Factory)LoaderInitializer.getImplInstance(Factory.class, "com.cobbleclub.client.render.shader.ShaderRegisterFactory");

   default ShaderProgram create(Identifier location, VertexFormat format) throws IOException {
      return this.create(location, format, true);
   }

   ShaderProgram create(Identifier var1, VertexFormat var2, boolean var3) throws IOException;

   default void register(Identifier location, VertexFormat format, Consumer<ShaderProgram> loadCallback) throws IOException {
      this.register(this.create(location, format), loadCallback);
   }

   void register(ShaderProgram var1, Consumer<ShaderProgram> var2);

   @Environment(EnvType.CLIENT)
   public interface Factory {
      void register(String var1, Consumer<ShaderRegister> var2);
   }
}
