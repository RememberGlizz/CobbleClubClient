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
import net.minecraft.class_1007;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_3264;
import net.minecraft.class_5602;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CobbleClubClient implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("cobbleclub");

   public void onInitializeClient() {
      class_3264 packType = class_3264.field_14188;
      ResourceManagerHelper.get(packType).registerReloadListener(new LayerDataLoader());
      ResourceManagerHelper.get(packType).registerReloadListener(new TeraMapLoader());
      ResourceManagerHelper.get(packType).registerReloadListener(new ArmorEquipmentLoader());
      LivingEntityFeatureRendererRegistrationCallback.EVENT.register((LivingEntityFeatureRendererRegistrationCallback)(entityType, entityRenderer, registrationHelper, context) -> {
         if (entityRenderer instanceof class_1007 playerRenderer) {
            registrationHelper.register(new CosmeticWingsLayer(playerRenderer, context.method_43338(), context.method_32167(class_5602.field_27559)));
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
         CobbleClubRenderTypes.teraFire = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_fire"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraWater = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_water"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraGrass = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_grass"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraElectric = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_electric"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraIce = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_ice"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraFighting = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_fighting"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraPoison = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_poison"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraGround = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_ground"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraFlying = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_flying"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraPsychic = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_psychic"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraBug = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_bug"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraRock = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_rock"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraGhost = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_ghost"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraDragon = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_dragon"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraDark = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_dark"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraSteel = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_steel"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraFairy = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_fairy"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraNormal = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_normal"), class_290.field_1580, true);
         CobbleClubRenderTypes.teraStellar = event.create(class_2960.method_60655("cobbleclub", "tera_crystal_stellar"), class_290.field_1580, true);
      });
      LOGGER.info("CobbleClub client initialized");
   }
}
