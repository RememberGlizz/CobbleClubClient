package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattleLogStore;
import com.cobbleclub.client.battle.BattleMessageParser;
import com.cobblemon.mod.common.client.net.battle.BattleMessageHandler;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_2588;
import net.minecraft.class_310;
import net.minecraft.class_7417;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {BattleMessageHandler.class},
   remap = false
)
public class BattleMessageHandlerMixin {
   @Inject(
      method = {"handle"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cobbleclub$onBattleMessage(BattleMessagePacket packet, class_310 client, CallbackInfo ci) {
      for(class_2561 message : packet.getMessages()) {
         if (message != null) {
            try {
               BattleMessageParser.accept(message);
               if (BattleConfig.get().enhancedLog) {
                  class_7417 var8 = message.method_10851();
                  String var10000;
                  if (var8 instanceof class_2588) {
                     class_2588 tc = (class_2588)var8;
                     var10000 = tc.method_11022();
                  } else {
                     var10000 = null;
                  }

                  String key = var10000;
                  BattleLogStore.add(message, key);
               }
            } catch (Throwable var9) {
            }
         }
      }

   }
}
