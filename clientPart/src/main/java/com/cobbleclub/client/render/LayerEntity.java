package com.cobbleclub.client.render;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;

@Environment(EnvType.CLIENT)
public class LayerEntity {
   public final PosableState state;
   public float ticks;
   protected double animSeconds = (double)0.0F;
   protected long lastTimeNs = -1L;

   public LayerEntity(PosableState state) {
      this.state = state;
   }

   public void render(RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, class_4587 poseStack, class_4597 buffer, int packedLight) {
      this.updateAnimTime();
   }

   public void render(String aspect, RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, float partialTicks, class_4587 poseStack, class_4597 buffer, int packedLight) {
      this.updateAnimTime();
   }

   protected void updateAnimTime() {
      if (!class_310.method_1551().method_1493()) {
         long now = System.nanoTime();
         if (this.lastTimeNs != -1L) {
            double deltaSeconds = (double)(now - this.lastTimeNs) / (double)1.0E9F;
            this.animSeconds += deltaSeconds;
         }

         this.lastTimeNs = now;
      } else {
         this.lastTimeNs = System.nanoTime();
      }

      float ticks = (float)(this.animSeconds * (double)20.0F);
      int age = (int)ticks;
      float pt = ticks - (float)age;
      this.state.updateAge(age);
      this.state.updatePartialTicks(pt);
   }
}
