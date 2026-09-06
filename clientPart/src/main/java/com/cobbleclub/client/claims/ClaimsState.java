package com.cobbleclub.client.claims;

import com.cobbleclub.client.ui.ConfiguredText;
import com.cobbleclub.client.ui.PreviewUi;
import com.cobbleclub.clubhouse.claims.protocol.ActionFeedback;
import com.cobbleclub.clubhouse.claims.protocol.BudgetInfo;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg;
import com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry;
import com.cobbleclub.clubhouse.claims.protocol.PermissionCatalogEntry;
import com.cobbleclub.clubhouse.claims.protocol.PermissionStateEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_746;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public final class ClaimsState {
   public final class_2561 title;
   public final String dimension;
   public final int playerX;
   public final int playerZ;
   public final int mapRadiusChunks;
   public final int minNameLength;
   public final int maxNameLength;
   public final String nameRegex;
   public final int maxMessageLength;
   public final int maxMapClaimChunksPerSide;
   public final List<PermRow> permissionCatalog;
   public final List<String> roles;
   public final List<class_2561> helpLines;
   private final Map<String, String> screenTextJson;
   public final String focusClaimId;
   public final String initialTab;
   public final boolean canBuyBlocks;
   public final String adminTargetName;
   public final int maxClaimDistanceChunks;
   public final boolean suppressOpenSound;
   public final boolean mapEnabled;
   public final boolean adminBypass;
   private BudgetInfo budget;
   private List<ClaimDetailEntry> myClaims;
   private List<ClaimDetailEntry> ownedClaims;
   private List<ClaimDetailEntry> trustedClaims;
   private List<MapClaimEntry> mapClaims;
   private int lastRevision;
   private class_2561 banner;
   private boolean bannerError;
   private long bannerUntilMillis;
   private final Map<String, String> optimisticPerms = new HashMap();
   private final Set<String> optimisticUntrusts = new HashSet();
   private String optimisticTrustClaim;
   private String optimisticTrustName;
   private ChunkRect pendingCreate;
   private static final DecimalFormat BLOCK_COUNT = new DecimalFormat("#,###");

   public ClaimsState(ClaimsOpenMsg msg) {
      this.title = PreviewUi.deserialize(msg.getTitleJson(), "");
      this.dimension = msg.getDimension() != null ? msg.getDimension() : "minecraft:overworld";
      this.playerX = msg.getPlayerX();
      this.playerZ = msg.getPlayerZ();
      this.mapRadiusChunks = Math.max(1, msg.getMapRadiusChunks());
      this.minNameLength = Math.max(1, msg.getMinNameLength());
      this.maxNameLength = msg.getMaxNameLength() > 0 ? msg.getMaxNameLength() : 32;
      this.nameRegex = msg.getNameRegex();
      this.maxMessageLength = msg.getMaxMessageLength() > 0 ? msg.getMaxMessageLength() : 64;
      this.maxMapClaimChunksPerSide = msg.getMaxMapClaimChunksPerSide() > 0 ? msg.getMaxMapClaimChunksPerSide() : 64;
      this.permissionCatalog = buildCatalog(msg.getPermissionCatalog());
      this.roles = msg.getRoles() != null ? List.copyOf(msg.getRoles()) : List.of("VISITOR", "TRUSTED", "OWNER");
      this.helpLines = buildHelp(msg.getHelpJson());
      this.screenTextJson = msg.getScreenTextJson() != null ? msg.getScreenTextJson() : Map.of();
      this.focusClaimId = msg.getFocusClaimId();
      this.initialTab = msg.getInitialTab();
      this.canBuyBlocks = Boolean.TRUE.equals(msg.getCanBuyBlocks());
      this.adminTargetName = msg.getAdminTargetName();
      this.maxClaimDistanceChunks = msg.getMaxClaimDistanceChunks() != null ? Math.max(0, msg.getMaxClaimDistanceChunks()) : 3;
      this.suppressOpenSound = Boolean.TRUE.equals(msg.getSuppressOpenSound());
      this.mapEnabled = !Boolean.FALSE.equals(msg.getMapEnabled());
      this.adminBypass = Boolean.TRUE.equals(msg.getAdminBypass());
      this.budget = msg.getBudget() != null ? msg.getBudget() : new BudgetInfo(0, 0);
      this.setClaims(filterClaims(msg.getMyClaims()));
      this.mapClaims = filterMapClaims(msg.getMapClaims());
   }

   public boolean applyState(ClaimsStateMsg msg) {
      if (msg.getRevision() <= this.lastRevision) {
         return false;
      } else {
         this.lastRevision = msg.getRevision();
         if (msg.getBudget() != null) {
            this.budget = msg.getBudget();
         }

         this.setClaims(filterClaims(msg.getMyClaims()));
         this.mapClaims = filterMapClaims(msg.getMapClaims());
         this.clearOptimistic();
         ActionFeedback feedback = msg.getFeedback();
         if (feedback != null && feedback.getMessageJson() != null) {
            this.showBanner(PreviewUi.deserialize(feedback.getMessageJson(), ""), !feedback.getOk());
         }

         return true;
      }
   }

   private void setClaims(List<ClaimDetailEntry> claims) {
      this.myClaims = claims;
      List<ClaimDetailEntry> owned = new ArrayList();
      List<ClaimDetailEntry> trusted = new ArrayList();

      for(ClaimDetailEntry claim : claims) {
         (claim.isOwner() ? owned : trusted).add(claim);
      }

      this.ownedClaims = owned;
      this.trustedClaims = trusted;
   }

   private static List<ClaimDetailEntry> filterClaims(List<ClaimDetailEntry> in) {
      List<ClaimDetailEntry> out = new ArrayList();
      if (in == null) {
         return out;
      } else {
         for(ClaimDetailEntry entry : in) {
            if (entry != null && entry.getClaimId() != null && entry.getBox() != null) {
               out.add(entry);
            }
         }

         return out;
      }
   }

   private static List<MapClaimEntry> filterMapClaims(List<MapClaimEntry> in) {
      List<MapClaimEntry> out = new ArrayList();
      if (in == null) {
         return out;
      } else {
         for(MapClaimEntry entry : in) {
            if (entry != null && entry.getClaimId() != null) {
               out.add(entry);
            }
         }

         return out;
      }
   }

   private static List<class_2561> buildHelp(List<String> json) {
      if (json == null) {
         return List.of();
      } else {
         List<class_2561> out = new ArrayList();

         for(String line : json) {
            out.add(line == null ? class_2561.method_43473() : PreviewUi.deserialize(line, ""));
         }

         return out;
      }
   }

   private static List<PermRow> buildCatalog(List<PermissionCatalogEntry> in) {
      List<PermRow> out = new ArrayList();
      if (in == null) {
         return out;
      } else {
         for(PermissionCatalogEntry entry : in) {
            if (entry != null && entry.getName() != null) {
               out.add(new PermRow(entry.getName(), PreviewUi.deserialize(entry.getDisplayNameJson(), entry.getName()), PreviewUi.deserialize(entry.getDescriptionJson(), ""), entry.getDefaultRole(), entry.getAssignableRoles() != null ? entry.getAssignableRoles() : List.of(), iconFor(entry.getIconItem())));
            }
         }

         return out;
      }
   }

   private static class_1799 iconFor(String itemId) {
      if (itemId == null) {
         return new class_1799(class_1802.field_8407);
      } else {
         class_2960 id = class_2960.method_12829(itemId.toLowerCase(Locale.ROOT));
         if (id == null) {
            return new class_1799(class_1802.field_8407);
         } else {
            class_1792 item = (class_1792)class_7923.field_41178.method_10223(id);
            return item == class_1802.field_8162 ? new class_1799(class_1802.field_8407) : new class_1799(item);
         }
      }
   }

   public boolean hasScreenText(String key) {
      return this.screenTextJson.containsKey(key);
   }

   public class_2561 screenText(String key, String... placeholders) {
      String json = (String)this.screenTextJson.get(key);
      return (class_2561)(json == null ? class_2561.method_43473() : ConfiguredText.fill(PreviewUi.deserialize(json, ""), placeholders));
   }

   public static String fmtBlocks(int n) {
      return BLOCK_COUNT.format((long)n);
   }

   public static boolean standingInside(ClaimDetailEntry claim) {
      if (!claim.getLocal()) {
         return false;
      } else {
         class_746 player = class_310.method_1551().field_1724;
         if (player != null && player.method_37908().method_27983().method_29177().toString().equals(claim.getDimension())) {
            int px = class_3532.method_15357(player.method_23317());
            int pz = class_3532.method_15357(player.method_23321());
            return px >= claim.getBox().getMinX() && px <= claim.getBox().getMaxX() && pz >= claim.getBox().getMinZ() && pz <= claim.getBox().getMaxZ();
         } else {
            return false;
         }
      }
   }

   public BudgetInfo budget() {
      return this.budget;
   }

   public int effectiveRemaining() {
      int remaining = this.budget.getRemaining();
      if (this.pendingCreate != null) {
         remaining -= rectArea(this.pendingCreate);
      }

      return remaining;
   }

   public static int rectArea(ChunkRect rect) {
      return (rect.getMaxCx() - rect.getMinCx() + 1) * (rect.getMaxCz() - rect.getMinCz() + 1) * 256;
   }

   public List<ClaimDetailEntry> ownedClaims() {
      return this.ownedClaims;
   }

   public List<ClaimDetailEntry> trustedClaims() {
      return this.trustedClaims;
   }

   public List<MapClaimEntry> mapClaims() {
      return this.mapClaims;
   }

   public boolean isOnMap(String claimId) {
      if (claimId == null) {
         return false;
      } else {
         for(MapClaimEntry entry : this.mapClaims) {
            if (claimId.equals(entry.getClaimId())) {
               return true;
            }
         }

         return false;
      }
   }

   public ClaimDetailEntry claimById(String id) {
      if (id == null) {
         return null;
      } else {
         for(ClaimDetailEntry entry : this.myClaims) {
            if (entry.getClaimId().equals(id)) {
               return entry;
            }
         }

         return null;
      }
   }

   public PermRow catalogEntry(String name) {
      for(PermRow row : this.permissionCatalog) {
         if (row.name().equals(name)) {
            return row;
         }
      }

      return null;
   }

   public String effectivePermRole(ClaimDetailEntry claim, String subId, String permission) {
      String overlay = (String)this.optimisticPerms.get(permKey(claim.getClaimId(), subId, permission));
      if (overlay != null) {
         return overlay.isEmpty() ? null : overlay;
      } else if (subId != null) {
         SubClaimEntry sub = subById(claim, subId);
         if (sub != null && sub.getOverrides() != null) {
            for(PermissionStateEntry entry : sub.getOverrides()) {
               if (entry != null && permission.equals(entry.getName())) {
                  return entry.getRequiredRole();
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         if (claim.getPermissions() != null) {
            for(PermissionStateEntry entry : claim.getPermissions()) {
               if (entry != null && permission.equals(entry.getName())) {
                  return entry.getRequiredRole();
               }
            }
         }

         PermRow row = this.catalogEntry(permission);
         return row != null ? row.defaultRole() : null;
      }
   }

   public static SubClaimEntry subById(ClaimDetailEntry claim, String subId) {
      if (claim.getSubClaims() != null && subId != null) {
         for(SubClaimEntry sub : claim.getSubClaims()) {
            if (sub != null && subId.equals(sub.getSubId())) {
               return sub;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public boolean isOptimisticallyUntrusted(String uuid) {
      return this.optimisticUntrusts.contains(uuid);
   }

   public String pendingTrustName(String claimId) {
      return claimId != null && claimId.equals(this.optimisticTrustClaim) ? this.optimisticTrustName : null;
   }

   public ChunkRect pendingCreate() {
      return this.pendingCreate;
   }

   public void optimisticSetPerm(String claimId, String subId, String permission, String role) {
      this.optimisticPerms.put(permKey(claimId, subId, permission), role == null ? "" : role);
   }

   public void optimisticUntrust(String uuid) {
      this.optimisticUntrusts.add(uuid);
   }

   public void optimisticTrust(String claimId, String name) {
      this.optimisticTrustClaim = claimId;
      this.optimisticTrustName = name;
   }

   public void optimisticCreate(ChunkRect rect) {
      this.pendingCreate = rect;
   }

   private void clearOptimistic() {
      this.optimisticPerms.clear();
      this.optimisticUntrusts.clear();
      this.optimisticTrustClaim = null;
      this.optimisticTrustName = null;
      this.pendingCreate = null;
   }

   private static String permKey(String claimId, String subId, String permission) {
      return claimId + "|" + (subId == null ? "" : subId) + "|" + permission;
   }

   public void showBanner(class_2561 text, boolean error) {
      if (text != null && !text.getString().isBlank()) {
         this.banner = text;
         this.bannerError = error;
         this.bannerUntilMillis = System.currentTimeMillis() + 4000L;
      }
   }

   public class_2561 banner() {
      return this.banner != null && System.currentTimeMillis() <= this.bannerUntilMillis ? this.banner : null;
   }

   public boolean bannerIsError() {
      return this.bannerError;
   }

   @Environment(EnvType.CLIENT)
   public static record PermRow(String name, class_2561 displayName, class_2561 description, String defaultRole, List<String> assignableRoles, class_1799 icon) {
   }
}
