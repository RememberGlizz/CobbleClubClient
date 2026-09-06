package com.cobbleclub.client.crate;

import com.cobbleclub.client.pokemonpreview.PokemonFormCatalog;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Environment(EnvType.CLIENT)
public final class CratePreviewNetworking {
   private static final Gson GSON = new Gson();
   private static CrateCatalog pending;
   private static List<CrateCatalog.Prize> pendingPrizes;

   private CratePreviewNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.playS2C().register(CratePreviewCatalogPayload.TYPE, CratePreviewCatalogPayload.STREAM_CODEC);
      PayloadTypeRegistry.playC2S().register(CrateTestRewardPayload.TYPE, CrateTestRewardPayload.STREAM_CODEC);
      ClientPlayNetworking.registerGlobalReceiver(CratePreviewCatalogPayload.TYPE, (payload, context) -> {
         try {
            CrateCatalog catalog = (CrateCatalog)GSON.fromJson(payload.json(), CrateCatalog.class);
            if (catalog == null) {
               return;
            }

            PokemonFormCatalog.configure(catalog.formAspects, catalog.formButtons, catalog.scopedForms);
            int totalParts = Math.max(1, catalog.totalParts);
            if (totalParts == 1) {
               reset();
               context.client().method_1507(new CratePreviewScreen(catalog));
               return;
            }

            if (catalog.part == 0) {
               pending = catalog;
               pendingPrizes = new ArrayList();
            } else if (pending == null || !Objects.equals(pending.id, catalog.id)) {
               reset();
               return;
            }

            if (catalog.prizes != null) {
               pendingPrizes.addAll(catalog.prizes);
            }

            if (catalog.part == totalParts - 1) {
               pending.prizes = pendingPrizes;
               CrateCatalog assembled = pending;
               reset();
               context.client().method_1507(new CratePreviewScreen(assembled));
            }
         } catch (Exception var5) {
            reset();
         }

      });
   }

   private static void reset() {
      pending = null;
      pendingPrizes = null;
   }

   static void requestTestReward(String crateId, int prizeIndex, boolean shiny) {
      if (crateId != null && ClientPlayNetworking.canSend(CrateTestRewardPayload.TYPE)) {
         ClientPlayNetworking.send(new CrateTestRewardPayload(crateId, prizeIndex, shiny));
      }

   }
}
