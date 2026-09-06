package com.cobbleclub.client.pokemonpreview;

import java.util.Comparator;
import kotlin.Metadata;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.internal.SourceDebugExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Metadata(
   mv = {2, 2, 0},
   k = 3,
   xi = 48
)
@Environment(EnvType.CLIENT)
@SourceDebugExtension({"SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareBy$2\n+ 2 PokemonFormCatalog.kt\ncom/cobbleclub/client/pokemonpreview/PokemonFormCatalog\n*L\n1#1,328:1\n224#2:329\n*E\n"})
public final class PokemonFormCatalog$prizeFormButtons$$inlined$sortedBy$1 implements Comparator<Object> {
   public final int compare(Object a, Object b) {
      PokemonFormCatalog.FormEntry it = (PokemonFormCatalog.FormEntry)a;
      int var4 = 0;
      Comparable var10000 = (Comparable)it.getLabel();
      it = (PokemonFormCatalog.FormEntry)b;
      Comparable var5 = var10000;
      var4 = 0;
      return ComparisonsKt.compareValues(var5, (Comparable)it.getLabel());
   }
}
