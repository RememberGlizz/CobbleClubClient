package com.cobbleclub.client.crate;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CrateCatalog {
   public String id;
   public String title;
   public String gradient;
   public boolean canTest;
   public boolean shinyPreview;
   public int part;
   public int totalParts;
   public List<String> formAspects;
   public List<String> formButtons;
   public Map<String, List<String>> scopedForms;
   public List<Prize> prizes;

   @Environment(EnvType.CLIENT)
   public static final class Prize {
      public String displayName;
      public String shinyDisplayName;
      public String species;
      public List<String> aspects;
      public String material;
      public int customModelData;
      public int shinyCustomModelData;
      public boolean shinyEligible;
      public int amount;
      public List<String> lore;
      public double chance;
      public String wornSlot;
      public List<String> evolutions;
   }
}
