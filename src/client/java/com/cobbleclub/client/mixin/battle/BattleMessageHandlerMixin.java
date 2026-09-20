package com.cobbleclub.client.mixin.battle;

import com.cobbleclub.client.battle.BattleConfig;
import com.cobbleclub.client.battle.BattleLogStore;
import com.cobbleclub.client.battle.BattleMessageParser;
import com.cobblemon.mod.common.client.net.battle.BattleMessageHandler;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = BattleMessageHandler.class, remap = false)
public class BattleMessageHandlerMixin {
    @Inject(method = "handle", at = @At("HEAD"), require = 0)
    private void cobbleclub$onBattleMessage(
            BattleMessagePacket packet,
            MinecraftClient client,
            CallbackInfo ci
    ) {
        for (Text message : packet.getMessages()) {
            if (message == null) {
                continue;
            }

            try {
                BattleMessageParser.accept(message);

                if (!BattleConfig.get().enhancedLog) {
                    continue;
                }

                TextContent content = message.getContent();
                String key = content instanceof TranslatableTextContent tc
                        ? tc.getKey()
                        : null;

                BattleLogStore.add(message, key);
            } catch (Throwable ignored) {
            }
        }
    }
}
