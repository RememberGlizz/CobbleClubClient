package com.cobbleclub.client.render.shader;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;

@Environment(EnvType.CLIENT)
public class ShaderRegisterFactory implements ShaderRegister.Factory {
   public void register(String modid, Consumer<ShaderRegister> consumer) {
      RegisterShaderEvent.EVENT.register(consumer::accept);
   }
}
