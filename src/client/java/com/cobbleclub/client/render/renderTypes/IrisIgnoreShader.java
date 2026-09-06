package com.cobbleclub.client.render.renderTypes;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class IrisIgnoreShader extends ShaderProgram {
   public IrisIgnoreShader(ResourceFactory provider, Identifier location, VertexFormat format) throws IOException {
      super(provider, location.toString(), format);
   }

   public boolean iris$shouldSkipThis() {
      return false;
   }
}
