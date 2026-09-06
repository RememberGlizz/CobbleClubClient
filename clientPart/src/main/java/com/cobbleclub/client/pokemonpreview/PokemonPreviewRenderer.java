package com.cobbleclub.client.pokemonpreview;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.item.PokemonItem.Companion;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector4f;

@Metadata(
   mv = {2, 2, 0},
   k = 1,
   xi = 48,
   d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J%\u0010\t\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\u00042\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006H\u0007¢\u0006\u0004\b\t\u0010\nJ\u0017\u0010\f\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\u0004H\u0007¢\u0006\u0004\b\f\u0010\rJu\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0016\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0005\u001a\u00020\u00042\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u00062\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u0014H\u0007¢\u0006\u0004\b\u001c\u0010\u001dJ%\u0010 \u001a\u00020\u00182\u0006\u0010\u001f\u001a\u00020\u001e2\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006H\u0002¢\u0006\u0004\b \u0010!R\u0014\u0010#\u001a\u00020\"8\u0002X\u0082T¢\u0006\u0006\n\u0004\b#\u0010$R\u0014\u0010%\u001a\u00020\"8\u0002X\u0082T¢\u0006\u0006\n\u0004\b%\u0010$R\u0016\u0010'\u001a\u00020&8\u0002@\u0002X\u0082\u000e¢\u0006\u0006\n\u0004\b'\u0010(R\u0016\u0010)\u001a\u00020\u00048\u0002@\u0002X\u0082\u000e¢\u0006\u0006\n\u0004\b)\u0010*R\u0016\u0010+\u001a\u00020\u00188\u0002@\u0002X\u0082\u000e¢\u0006\u0006\n\u0004\b+\u0010,¨\u0006-"},
   d2 = {"Lcom/cobbleclub/client/pokemonpreview/PokemonPreviewRenderer;", "", "<init>", "()V", "", "species", "", "aspects", "Lnet/minecraft/class_1799;", "modelStack", "(Ljava/lang/String;Ljava/util/List;)Lnet/minecraft/class_1799;", "", "dexNumber", "(Ljava/lang/String;)I", "Lnet/minecraft/class_332;", "g", "x0", "y0", "x1", "y1", "", "yaw", "pitch", "zoom", "", "idleFly", "partialTicks", "", "render", "(Lnet/minecraft/class_332;IIIIFFFLjava/lang/String;Ljava/util/List;ZF)V", "Lnet/minecraft/class_2960;", "id", "hasHoverPose", "(Lnet/minecraft/class_2960;Ljava/util/List;)Z", "", "SCALE_FRAC", "D", "VISUAL_CENTER_K", "Lcom/cobblemon/mod/common/client/render/models/blockbench/FloatingState;", "state", "Lcom/cobblemon/mod/common/client/render/models/blockbench/FloatingState;", "lastKey", "Ljava/lang/String;", "hoverPose", "Z", "clubhouse-client-cobbleclub_client"}
)
@Environment(EnvType.CLIENT)
public final class PokemonPreviewRenderer {
   @NotNull
   public static final PokemonPreviewRenderer INSTANCE = new PokemonPreviewRenderer();
   private static final double SCALE_FRAC = 0.021;
   private static final double VISUAL_CENTER_K = (double)21.5F;
   @NotNull
   private static FloatingState state = new FloatingState();
   @NotNull
   private static String lastKey = "";
   private static boolean hoverPose;

   private PokemonPreviewRenderer() {
   }

   @JvmStatic
   @NotNull
   public static final class_1799 modelStack(@NotNull String species, @NotNull List<String> aspects) {
      Intrinsics.checkNotNullParameter(species, "species");
      Intrinsics.checkNotNullParameter(aspects, "aspects");
      class_2960 var10000 = class_2960.method_12829(species);
      if (var10000 == null) {
         class_1799 var6 = class_1799.field_8037;
         Intrinsics.checkNotNullExpressionValue(var6, "EMPTY");
         return var6;
      } else {
         class_2960 id = var10000;
         Species var4 = PokemonSpecies.getByIdentifier(id);
         if (var4 == null) {
            class_1799 var5 = class_1799.field_8037;
            Intrinsics.checkNotNullExpressionValue(var5, "EMPTY");
            return var5;
         } else {
            Species sp = var4;
            Set<String> safeAspects = aspects.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            return PokemonItem.Companion.from(sp, safeAspects, 1, null);
         }
      }
   }

   @JvmStatic
   public static final int dexNumber(@NotNull String species) {
      Intrinsics.checkNotNullParameter(species, "species");
      class_2960 var10000 = class_2960.method_12829(species);
      if (var10000 == null) {
         return Integer.MAX_VALUE;
      } else {
         class_2960 id = var10000;
         Species var2 = PokemonSpecies.getByIdentifier(id);
         return var2 != null ? var2.getNationalPokedexNumber() : Integer.MAX_VALUE;
      }
   }

   @JvmStatic
   public static final void render(@NotNull class_332 g, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, @NotNull String species, @NotNull List<String> aspects, boolean idleFly, float partialTicks) {
      Intrinsics.checkNotNullParameter(g, "g");
      Intrinsics.checkNotNullParameter(species, "species");
      Intrinsics.checkNotNullParameter(aspects, "aspects");
      class_2960 var10000 = class_2960.method_12829(species);
      if (var10000 != null) {
         class_2960 id = var10000;
         Species var22 = PokemonSpecies.getByIdentifier(id);
         if (var22 != null) {
            Species sp = var22;
            List<String> safeAspects = aspects.stream().filter(Objects::nonNull).toList();
            String key = species + "|" + safeAspects.stream().sorted().collect(Collectors.joining(","));
            if (!Intrinsics.areEqual(key, lastKey)) {
               state = new FloatingState();
               lastKey = key;
               hoverPose = INSTANCE.hasHoverPose(id, safeAspects);
            }

            double paneH = (double)(y1 - y0);
            float outerScale = (float)(paneH * 0.021 * (double)zoom);
            double anchorY = (double)(y0 + y1) / (double)2.0F - (double)outerScale * (double)21.5F;
            g.method_44379(x0, y0, x1, y1);
            g.method_51448().method_22903();
            g.method_51448().method_22904((double)(x0 + x1) / (double)2.0F, anchorY, (double)1000.0F);
            g.method_51448().method_22905(outerScale, outerScale, outerScale);
            Quaternionf rotation = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)pitch), (float)Math.toRadians((double)yaw), 0.0F);
            RenderablePokemon var23 = new RenderablePokemon(sp, Set.copyOf(safeAspects), class_1799.field_8037);
            class_4587 var10001 = g.method_51448();
            Intrinsics.checkNotNullExpressionValue(var10001, "pose(...)");
            Intrinsics.checkNotNull(rotation);
            PokemonGuiUtilsKt.drawProfilePokemon(
               var23,
               var10001,
               rotation,
               idleFly && hoverPose ? PoseType.HOVER : PoseType.PROFILE,
               state,
               partialTicks,
               20.0F,
               true,
               false,
               1.0F,
               1.0F,
               1.0F,
               1.0F,
               0.0F,
               0.0F
            );
            g.method_51452();
            g.method_51448().method_22909();
            g.method_44380();
         }
      }
   }

   private final boolean hasHoverPose(class_2960 id, List<String> aspects) {
      boolean var3;
      try {
         state.setCurrentAspects(CollectionsKt.toSet((Iterable)aspects));
         var3 = VaryingModelRepository.INSTANCE.getPoser(id, (PosableState)state).getPose(PoseType.HOVER) != null;
      } catch (Exception var5) {
         var3 = false;
      }

      return var3;
   }
}
