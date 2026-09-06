package com.cobbleclub.client.render.renderTypes;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_5912;
import net.minecraft.class_5944;

@Environment(EnvType.CLIENT)
public class IrisIgnoreShader extends class_5944 {
   public IrisIgnoreShader(class_5912 provider, class_2960 location, class_293 format) throws IOException {
      super(provider, location.toString(), format);
   }

   public boolean iris$shouldSkipThis() {
      return false;
   }
}
