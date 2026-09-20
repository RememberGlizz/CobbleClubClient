/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobbleclub.clubhouse.claims.protocol.ActionFeedback
 *  com.cobbleclub.clubhouse.claims.protocol.BudgetInfo
 *  com.cobbleclub.clubhouse.claims.protocol.ChunkRect
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsOpenMsg
 *  com.cobbleclub.clubhouse.claims.protocol.ClaimsStateMsg
 *  com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry
 *  com.cobbleclub.clubhouse.claims.protocol.PermissionCatalogEntry
 *  com.cobbleclub.clubhouse.claims.protocol.PermissionStateEntry
 *  com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_746
 *  net.minecraft.class_7923
 */
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

@Environment(value=EnvType.CLIENT)
public final class ClaimsState {
    public final Text title;
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
    public final List<Text> helpLines;
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
    private Text banner;
    private boolean bannerError;
    private long bannerUntilMillis;
    private final Map<String, String> optimisticPerms = new HashMap<String, String>();
    private final Set<String> optimisticUntrusts = new HashSet<String>();
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
        this.permissionCatalog = ClaimsState.buildCatalog(msg.getPermissionCatalog());
        this.roles = msg.getRoles() != null ? List.copyOf(msg.getRoles()) : List.of("VISITOR", "TRUSTED", "OWNER");
        this.helpLines = ClaimsState.buildHelp(msg.getHelpJson());
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
        this.setClaims(ClaimsState.filterClaims(msg.getMyClaims()));
        this.mapClaims = ClaimsState.filterMapClaims(msg.getMapClaims());
    }

    public boolean applyState(ClaimsStateMsg msg) {
        if (msg.getRevision() <= this.lastRevision) {
            return false;
        }
        this.lastRevision = msg.getRevision();
        if (msg.getBudget() != null) {
            this.budget = msg.getBudget();
        }
        this.setClaims(ClaimsState.filterClaims(msg.getMyClaims()));
        this.mapClaims = ClaimsState.filterMapClaims(msg.getMapClaims());
        this.clearOptimistic();
        ActionFeedback feedback = msg.getFeedback();
        if (feedback != null && feedback.getMessageJson() != null) {
            this.showBanner(PreviewUi.deserialize(feedback.getMessageJson(), ""), !feedback.getOk());
        }
        return true;
    }

    private void setClaims(List<ClaimDetailEntry> claims) {
        this.myClaims = claims;
        ArrayList<ClaimDetailEntry> owned = new ArrayList<ClaimDetailEntry>();
        ArrayList<ClaimDetailEntry> trusted = new ArrayList<ClaimDetailEntry>();
        for (ClaimDetailEntry claim : claims) {
            (claim.isOwner() ? owned : trusted).add(claim);
        }
        this.ownedClaims = owned;
        this.trustedClaims = trusted;
    }

    private static List<ClaimDetailEntry> filterClaims(List<ClaimDetailEntry> in) {
        ArrayList<ClaimDetailEntry> out = new ArrayList<ClaimDetailEntry>();
        if (in == null) {
            return out;
        }
        for (ClaimDetailEntry entry : in) {
            if (entry == null || entry.getClaimId() == null || entry.getBox() == null) continue;
            out.add(entry);
        }
        return out;
    }

    private static List<MapClaimEntry> filterMapClaims(List<MapClaimEntry> in) {
        ArrayList<MapClaimEntry> out = new ArrayList<MapClaimEntry>();
        if (in == null) {
            return out;
        }
        for (MapClaimEntry entry : in) {
            if (entry == null || entry.getClaimId() == null) continue;
            out.add(entry);
        }
        return out;
    }

    private static List<Text> buildHelp(List<String> json) {
        if (json == null) {
            return List.of();
        }
        ArrayList<Text> out = new ArrayList<Text>();
        for (String line : json) {
            out.add((Text)(line == null ? Text.empty() : PreviewUi.deserialize(line, "")));
        }
        return out;
    }

    private static List<PermRow> buildCatalog(List<PermissionCatalogEntry> in) {
        ArrayList<PermRow> out = new ArrayList<PermRow>();
        if (in == null) {
            return out;
        }
        for (PermissionCatalogEntry entry : in) {
            if (entry == null || entry.getName() == null) continue;
            out.add(new PermRow(entry.getName(), PreviewUi.deserialize(entry.getDisplayNameJson(), entry.getName()), PreviewUi.deserialize(entry.getDescriptionJson(), ""), entry.getDefaultRole(), entry.getAssignableRoles() != null ? entry.getAssignableRoles() : List.of(), ClaimsState.iconFor(entry.getIconItem())));
        }
        return out;
    }

    private static ItemStack iconFor(String itemId) {
        if (itemId == null) {
            return new ItemStack((ItemConvertible)Items.PAPER);
        }
        Identifier id = Identifier.tryParse((String)itemId.toLowerCase(Locale.ROOT));
        if (id == null) {
            return new ItemStack((ItemConvertible)Items.PAPER);
        }
        Item item = (Item)Registries.ITEM.get(id);
        return item == Items.AIR ? new ItemStack((ItemConvertible)Items.PAPER) : new ItemStack((ItemConvertible)item);
    }

    public boolean hasScreenText(String key) {
        return this.screenTextJson.containsKey(key);
    }

    public Text screenText(String key, String ... placeholders) {
        String json = this.screenTextJson.get(key);
        return json == null ? Text.empty() : ConfiguredText.fill(PreviewUi.deserialize(json, ""), placeholders);
    }

    public static String friendlyWorldName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown World";
        }
        String id = raw;
        int colon = id.indexOf(':');
        if (colon >= 0 && colon + 1 < id.length()) {
            id = id.substring(colon + 1);
        }
        id = id.replace('-', '_');
        if ("overworld".equalsIgnoreCase(id)) return "Overworld";
        if ("the_nether".equalsIgnoreCase(id) || "nether".equalsIgnoreCase(id)) return "The Nether";
        if ("the_end".equalsIgnoreCase(id) || "end".equalsIgnoreCase(id)) return "The End";
        if (id.toLowerCase().endsWith("world") && id.length() > 5) {
            id = id.substring(0, id.length() - 5) + "_world";
        }
        StringBuilder out = new StringBuilder();
        for (String part : id.split("_+")) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1).toLowerCase());
        }
        return out.isEmpty() ? "Unknown World" : out.toString();
    }

    public static String fmtBlocks(int n) {
        return BLOCK_COUNT.format(n);
    }

    public static boolean standingInside(ClaimDetailEntry claim) {
        if (!claim.getLocal()) {
            return false;
        }
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.getWorld().getRegistryKey().getValue().toString().equals(claim.getDimension())) {
            int px = MathHelper.floor((double)player.getX());
            int pz = MathHelper.floor((double)player.getZ());
            return px >= claim.getBox().getMinX() && px <= claim.getBox().getMaxX() && pz >= claim.getBox().getMinZ() && pz <= claim.getBox().getMaxZ();
        }
        return false;
    }

    public BudgetInfo budget() {
        return this.budget;
    }

    public int effectiveRemaining() {
        int remaining = this.budget.getRemaining();
        if (this.pendingCreate != null) {
            remaining -= ClaimsState.rectArea(this.pendingCreate);
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
        }
        for (MapClaimEntry entry : this.mapClaims) {
            if (!claimId.equals(entry.getClaimId())) continue;
            return true;
        }
        return false;
    }

    public ClaimDetailEntry claimById(String id) {
        if (id == null) {
            return null;
        }
        for (ClaimDetailEntry entry : this.myClaims) {
            if (!entry.getClaimId().equals(id)) continue;
            return entry;
        }
        return null;
    }

    public PermRow catalogEntry(String name) {
        for (PermRow row : this.permissionCatalog) {
            if (!row.name().equals(name)) continue;
            return row;
        }
        return null;
    }

    public String effectivePermRole(ClaimDetailEntry claim, String subId, String permission) {
        PermRow row;
        String overlay = this.optimisticPerms.get(ClaimsState.permKey(claim.getClaimId(), subId, permission));
        if (overlay != null) {
            return overlay.isEmpty() ? null : overlay;
        }
        if (subId != null) {
            SubClaimEntry sub = ClaimsState.subById(claim, subId);
            if (sub != null && sub.getOverrides() != null) {
                for (PermissionStateEntry entry : sub.getOverrides()) {
                    if (entry == null || !permission.equals(entry.getName())) continue;
                    return entry.getRequiredRole();
                }
                return null;
            }
            return null;
        }
        if (claim.getPermissions() != null) {
            for (PermissionStateEntry entry : claim.getPermissions()) {
                if (entry == null || !permission.equals(entry.getName())) continue;
                return entry.getRequiredRole();
            }
        }
        return (row = this.catalogEntry(permission)) != null ? row.defaultRole() : null;
    }

    public static SubClaimEntry subById(ClaimDetailEntry claim, String subId) {
        if (claim.getSubClaims() != null && subId != null) {
            for (SubClaimEntry sub : claim.getSubClaims()) {
                if (sub == null || !subId.equals(sub.getSubId())) continue;
                return sub;
            }
            return null;
        }
        return null;
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
        this.optimisticPerms.put(ClaimsState.permKey(claimId, subId, permission), role == null ? "" : role);
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

    public void showBanner(Text text, boolean error) {
        if (text != null && !text.getString().isBlank()) {
            this.banner = text;
            this.bannerError = error;
            this.bannerUntilMillis = System.currentTimeMillis() + 4000L;
        }
    }

    public Text banner() {
        return this.banner != null && System.currentTimeMillis() <= this.bannerUntilMillis ? this.banner : null;
    }

    public boolean bannerIsError() {
        return this.bannerError;
    }

    @Environment(value=EnvType.CLIENT)
    public record PermRow(String name, Text displayName, Text description, String defaultRole, List<String> assignableRoles, ItemStack icon) {
    }
}

