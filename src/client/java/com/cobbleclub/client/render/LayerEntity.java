/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.client.entity.PokemonClientDelegate
 *  com.cobblemon.mod.common.client.render.models.blockbench.PosableState
 *  com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
 *  com.cobblemon.mod.common.entity.pokemon.PokemonEntity
 *  com.cobblemon.mod.common.pokemon.Pokemon
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 */
package com.cobbleclub.client.render;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;

@Environment(value=EnvType.CLIENT)
public class LayerEntity {
    public final PosableState state;
    public float ticks;
    protected double animSeconds = 0.0;
    protected long lastTimeNs = -1L;

    public LayerEntity(PosableState state) {
        this.state = state;
    }

    public void render(RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, MatrixStack poseStack, VertexConsumerProvider buffer, int packedLight) {
        this.updateAnimTime();
    }

    public void render(String aspect, RenderContext context, PokemonClientDelegate clientDelegate, PokemonEntity entity, Pokemon pokemon, float entityYaw, float partialTicks, MatrixStack poseStack, VertexConsumerProvider buffer, int packedLight) {
        this.updateAnimTime();
    }

    protected void updateAnimTime() {
        if (!MinecraftClient.getInstance().isPaused()) {
            long now = System.nanoTime();
            if (this.lastTimeNs != -1L) {
                double deltaSeconds = (double)(now - this.lastTimeNs) / 1.0E9;
                this.animSeconds += deltaSeconds;
            }
            this.lastTimeNs = now;
        } else {
            this.lastTimeNs = System.nanoTime();
        }
        float ticks = (float)(this.animSeconds * 20.0);
        int age = (int)ticks;
        float pt = ticks - (float)age;
        this.state.updateAge(age);
        this.state.updatePartialTicks(pt);
    }
}

