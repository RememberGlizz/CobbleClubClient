package com.cobbleclub.client;

import com.cobbleclub.client.battle.BattleClientInit;
import com.cobbleclub.client.claims.ClaimsNetworking;
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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CobbleClubClient implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("cobbleclub");

   public void onInitializeClient() {
      ResourceType packType = ResourceType.CLIENT_RESOURCES;
      ResourceManagerHelper.get(packType).registerReloadListener(new LayerDataLoader());
      ResourceManagerHelper.get(packType).registerReloadListener(new TeraMapLoader());
      ResourceManagerHelper.get(packType).registerReloadListener(new ArmorEquipmentLoader());
      LivingEntityFeatureRendererRegistrationCallback.EVENT.register((LivingEntityFeatureRendererRegistrationCallback)(entityType, entityRenderer, registrationHelper, context) -> {
         if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
            registrationHelper.register(new CosmeticWingsLayer(playerRenderer, context.getHeldItemRenderer(), context.getPart(EntityModelLayers.ELYTRA)));
         }

      });
      WardrobeNetworking.init();
      GearPreviewNetworking.init();
      PokemonPreviewNetworking.init();
      CratePreviewNetworking.init();
      TagsNetworking.init();
      ClaimsNetworking.init();
      DashboardNetworking.init();
      KitsNetworking.init();
      ClientModHandshake.init();
      BattleClientInit.init();
      RegisterShaderEvent.EVENT.register((RegisterShaderEvent)(event) -> {
         CobbleClubRenderTypes.teraFire = event.create(Identifier.of("cobbleclub", "tera_crystal_fire"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraWater = event.create(Identifier.of("cobbleclub", "tera_crystal_water"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraGrass = event.create(Identifier.of("cobbleclub", "tera_crystal_grass"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraElectric = event.create(Identifier.of("cobbleclub", "tera_crystal_electric"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraIce = event.create(Identifier.of("cobbleclub", "tera_crystal_ice"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraFighting = event.create(Identifier.of("cobbleclub", "tera_crystal_fighting"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraPoison = event.create(Identifier.of("cobbleclub", "tera_crystal_poison"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraGround = event.create(Identifier.of("cobbleclub", "tera_crystal_ground"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraFlying = event.create(Identifier.of("cobbleclub", "tera_crystal_flying"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraPsychic = event.create(Identifier.of("cobbleclub", "tera_crystal_psychic"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraBug = event.create(Identifier.of("cobbleclub", "tera_crystal_bug"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraRock = event.create(Identifier.of("cobbleclub", "tera_crystal_rock"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraGhost = event.create(Identifier.of("cobbleclub", "tera_crystal_ghost"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraDragon = event.create(Identifier.of("cobbleclub", "tera_crystal_dragon"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraDark = event.create(Identifier.of("cobbleclub", "tera_crystal_dark"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraSteel = event.create(Identifier.of("cobbleclub", "tera_crystal_steel"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraFairy = event.create(Identifier.of("cobbleclub", "tera_crystal_fairy"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraNormal = event.create(Identifier.of("cobbleclub", "tera_crystal_normal"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
         CobbleClubRenderTypes.teraStellar = event.create(Identifier.of("cobbleclub", "tera_crystal_stellar"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, true);
      });
      LOGGER.info("CobbleClub client initialized");
   }
}
