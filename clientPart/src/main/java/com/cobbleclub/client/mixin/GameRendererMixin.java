package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.renderTypes.IrisIgnoreShader;
import com.cobbleclub.client.render.shader.RegisterShaderEvent;
import com.cobbleclub.client.render.shader.ShaderRegister;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_5912;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_757.class})
public abstract class GameRendererMixin {
   @Inject(
      method = {"reloadShaders"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GameRenderer;loadBlurEffect(Lnet/minecraft/server/packs/resources/ResourceProvider;)V"
)}
   )
   private void onShaderLoad(final class_5912 provider, CallbackInfo info, @Local(ordinal = 1) final List<Pair<class_5944, Consumer<class_5944>>> programs) throws IOException {
      ShaderRegister register = new ShaderRegister() {
         public class_5944 create(class_2960 location, class_293 format, boolean irisIgnore) throws IOException {
            return (class_5944)(irisIgnore ? new IrisIgnoreShader(provider, location, format) : new FabricShaderProgram(provider, location, format));
         }

         public void register(class_5944 shaderInstance, Consumer<class_5944> loadCallback) {
            programs.add(Pair.of(shaderInstance, loadCallback));
         }
      };
      ((RegisterShaderEvent)RegisterShaderEvent.EVENT.invoker()).create(register);
   }
}
