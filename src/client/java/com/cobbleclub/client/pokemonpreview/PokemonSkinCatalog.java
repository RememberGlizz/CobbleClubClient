package com.cobbleclub.client.pokemonpreview;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PokemonSkinCatalog {
   public List<Family> families;
   public Map<String, Integer> sortKeys;
   public List<String> idleFly;
   public List<String> formAspects;
   public List<String> formButtons;
   public Map<String, List<String>> scopedForms;

   @Environment(EnvType.CLIENT)
   public static class Family {
      public String id;
      public String displayName;
      public String shortName;
      public String skinAspect;
      public List<Entry> entries;
   }

   @Environment(EnvType.CLIENT)
   public static class Entry {
      public String species;
      public List<String> aspects;
      public String displayName;
   }
}
