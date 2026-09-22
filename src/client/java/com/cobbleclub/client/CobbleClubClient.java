/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
 *  net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
 *  net.fabricmc.fabric.api.resource.ResourceManagerHelper
 *  net.minecraft.class_1007
 *  net.minecraft.class_290
 *  net.minecraft.class_2960
 *  net.minecraft.class_3264
 *  net.minecraft.class_3883
 *  net.minecraft.class_3887
 *  net.minecraft.class_5602
 *  net.minecraft.class_591
 *  net.minecraft.class_742
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.cobbleclub.client;

import java.util.Locale;
import com.cobbleclub.client.battle.BattleClientInit;
import com.cobbleclub.client.claims.ClaimsNetworking;
import com.cobbleclub.client.hud.FeatherboardHud;
import com.cobbleclub.client.crate.CratePreviewNetworking;
import com.cobbleclub.client.dashboard.DashboardNetworking;
import com.cobbleclub.client.gearpreview.GearPreviewNetworking;
import com.cobbleclub.client.kits.KitsNetworking;
import com.cobbleclub.client.net.ClientModHandshake;
import com.cobbleclub.client.pokemonpreview.PokemonPreviewNetworking;
import com.cobbleclub.client.render.ArmorEquipmentLoader;
import com.cobbleclub.client.render.CosmeticWingsLayer;
import com.cobbleclub.client.render.TeraMapLoader;
import com.cobbleclub.client.render.renderTypes.CobbleClubRenderTypes;
import com.cobbleclub.client.render.shader.RegisterShaderEvent;
import com.cobbleclub.client.rp.LayerDataLoader;
import com.cobbleclub.client.tags.TagsNetworking;
import com.cobbleclub.client.wardrobe.WardrobeNetworking;
import com.cobbleclub.client.world.ManagedBorderClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceType;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public class CobbleClubClient
implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"cobbleclub");
    private static final long RECOMMENDED_MEMORY_BYTES = 8L * 1024L * 1024L * 1024L;
    private static boolean memoryWarningShown;

    public void onInitializeClient() {
        ResourceType packType = ResourceType.CLIENT_RESOURCES;
        ResourceManagerHelper.get((ResourceType)packType).registerReloadListener((IdentifiableResourceReloadListener)new LayerDataLoader());
        ResourceManagerHelper.get((ResourceType)packType).registerReloadListener((IdentifiableResourceReloadListener)new TeraMapLoader());
        ResourceManagerHelper.get((ResourceType)packType).registerReloadListener((IdentifiableResourceReloadListener)new ArmorEquipmentLoader());
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer) {
                PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer)entityRenderer;
                registrationHelper.register((FeatureRenderer)new CosmeticWingsLayer((FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>)playerRenderer, context.getHeldItemRenderer(), context.getPart(EntityModelLayers.ELYTRA)));
            }
        });
        WardrobeNetworking.init();
        GearPreviewNetworking.init();
        PokemonPreviewNetworking.init();
        CratePreviewNetworking.init();
        TagsNetworking.init();
        ClaimsNetworking.init();
        FeatherboardHud.init();
        DashboardNetworking.init();
        KitsNetworking.init();
        ClientModHandshake.init();
        ManagedBorderClient.init();
        BattleClientInit.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!memoryWarningShown && client.player != null && client.world != null) {
                memoryWarningShown = true;
                long maxMemory = Runtime.getRuntime().maxMemory();
                if (maxMemory < RECOMMENDED_MEMORY_BYTES) {
                    double allocatedGb = maxMemory / (1024.0D * 1024.0D * 1024.0D);
                    client.player.sendMessage(
                            Text.literal(String.format(Locale.ROOT,
                                    "⚠ CobbleClub recommends 8 GB RAM. Your launcher currently allows about %.1f GB.",
                                    allocatedGb))
                                    .formatted(Formatting.GOLD, Formatting.BOLD),
                            false
                    );
                    client.player.sendMessage(
                            Text.literal("Increase it in your Modrinth instance settings under Java / Memory for the best experience.")
                                    .formatted(Formatting.YELLOW),
                            false
                    );
                }
            }
        });
        RegisterShaderEvent.EVENT.register(event -> {
            CobbleClubRenderTypes.teraFire = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_fire"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraWater = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_water"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraGrass = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_grass"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraElectric = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_electric"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraIce = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_ice"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraFighting = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_fighting"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraPoison = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_poison"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraGround = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_ground"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraFlying = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_flying"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraPsychic = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_psychic"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraBug = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_bug"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraRock = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_rock"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraGhost = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_ghost"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraDragon = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_dragon"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraDark = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_dark"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraSteel = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_steel"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraFairy = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_fairy"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraNormal = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_normal"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
            CobbleClubRenderTypes.teraStellar = event.create(Identifier.of((String)"cobbleclub", (String)"tera_crystal_stellar"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
        });
        LOGGER.info("CobbleClub client initialized");
    }
}

