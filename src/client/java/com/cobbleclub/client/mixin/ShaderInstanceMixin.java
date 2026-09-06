package com.cobbleclub.client.mixin;

import com.cobbleclub.client.render.renderTypes.IrisIgnoreShader;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ShaderProgram.class})
public abstract class ShaderInstanceMixin {
   @Shadow
   @Final
   private String name;

   @WrapOperation(
      method = {"<init>"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/Identifier;ofVanilla(Ljava/lang/String;)Lnet/minecraft/util/Identifier;"
)},
      allow = 1
   )
   private Identifier modifyId(String id, Operation<Identifier> original) {
      return (Object)this instanceof IrisIgnoreShader ? FabricShaderProgram.rewriteAsId(id, this.name) : (Identifier)original.call(new Object[]{id});
   }
}
