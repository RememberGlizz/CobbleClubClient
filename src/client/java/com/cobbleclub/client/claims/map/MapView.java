package com.cobbleclub.client.claims.map;

import com.cobbleclub.client.claims.ClaimsState;
import com.cobbleclub.client.claims.map.ChunkSelection;
import com.cobbleclub.client.claims.map.MapTileCache;
import com.cobbleclub.clubhouse.claims.protocol.BoxInfo;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class MapView {
    private static final double[] ZOOMS = new double[]{0.5, 1.0, 2.0, 4.0};
    private static final int GRID_LINE = 0x22FFFFFF;
    private static final int GRID_LINE_MAJOR = 0x44FFFFFF;
    private static final int OWN_FILL = 815615709;
    private static final int OWN_OUTLINE = -6467875;
    private static final int TRUSTED_FILL = 809481910;
    private static final int TRUSTED_OUTLINE = -12601674;
    private static final int OTHER_FILL = 820017727;
    private static final int OTHER_OUTLINE = -5218256;
    private static final int ADMIN_FILL = 819348816;
    private static final int ADMIN_OUTLINE = -2734768;
    private static final int SUB_OUTLINE = -4668724;
    private static final int HOVER_BOOST = 0x18FFFFFF;
    private static final int SELECT_VALID_FILL = 1212262495;
    private static final int SELECT_VALID_OUTLINE = -12474273;
    private static final int SELECT_INVALID_FILL = 1222002000;
    private static final int SELECT_INVALID_OUTLINE = -2734768;
    private static final int PENDING_OUTLINE = -2053377;
    private static final int VOID_BASE = -16315620;
    private static final int VOID_CHECKER = -16052182;
    private final ClaimsState state;
    private final MapTileCache tiles;
    private final ChunkSelection selection = new ChunkSelection();
    private double centerX;
    private double centerZ;
    private int zoomIndex = 2;
    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private static final double DRAG_THRESHOLD_PX = 4.0;
    private int pressButton = -1;
    private double pressX;
    private double pressY;
    private boolean draggingPan;
    private boolean draggingSelect;
    private double lastMouseX;
    private double lastMouseY;
    private static final double CORNER_GRAB_PX = 7.0;

    public MapView(ClaimsState state, MapTileCache tiles) {
        this.state = state;
        this.tiles = tiles;
        this.centerX = state.playerX;
        this.centerZ = state.playerZ;
    }

    public ChunkSelection selection() {
        return this.selection;
    }

    public void setBounds(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= (double)this.x0 && mouseX < (double)this.x1 && mouseY >= (double)this.y0 && mouseY < (double)this.y1;
    }

    private double zoom() {
        return ZOOMS[this.zoomIndex];
    }

    public String zoomLabel() {
        double z = this.zoom();
        String var10000 = z == Math.floor(z) ? String.valueOf((int)z) : String.valueOf(z);
        return var10000 + "x";
    }

    public double worldToScreenX(double wx) {
        return (double)(this.x0 + this.x1) / 2.0 + (wx - this.centerX) * this.zoom();
    }

    public double worldToScreenY(double wz) {
        return (double)(this.y0 + this.y1) / 2.0 + (wz - this.centerZ) * this.zoom();
    }

    public double screenToWorldX(double sx) {
        return this.centerX + (sx - (double)(this.x0 + this.x1) / 2.0) / this.zoom();
    }

    public double screenToWorldZ(double sy) {
        return this.centerZ + (sy - (double)(this.y0 + this.y1) / 2.0) / this.zoom();
    }

    public int cursorBlockX(double mouseX) {
        return MathHelper.floor((double)this.screenToWorldX(mouseX));
    }

    public int cursorBlockZ(double mouseY) {
        return MathHelper.floor((double)this.screenToWorldZ(mouseY));
    }

    private void clampCenter() {
        this.centerX = MapView.clampAxis(this.centerX, this.tiles.atlasMinBlockX(), this.tiles.atlasMinBlockX() + this.tiles.atlasSizePx(), (double)(this.x1 - this.x0) / this.zoom());
        this.centerZ = MapView.clampAxis(this.centerZ, this.tiles.atlasMinBlockZ(), this.tiles.atlasMinBlockZ() + this.tiles.atlasSizePx(), (double)(this.y1 - this.y0) / this.zoom());
    }

    private static double clampAxis(double center, int min, int max, double viewSpanBlocks) {
        double half = viewSpanBlocks / 2.0;
        return viewSpanBlocks >= (double)(max - min) ? (double)(min + max) / 2.0 : MathHelper.clamp((double)center, (double)((double)min + half), (double)((double)max - half));
    }

    public void centerOnPlayer() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            this.centerOn(player.getX(), player.getZ());
        } else {
            this.centerOn(this.state.playerX, this.state.playerZ);
        }
    }

    public void centerOn(double blockX, double blockZ) {
        this.centerX = blockX;
        this.centerZ = blockZ;
        this.clampCenter();
    }

    public void panBy(double dxBlocks, double dzBlocks) {
        this.centerX += dxBlocks;
        this.centerZ += dzBlocks;
        this.clampCenter();
    }

    public void keyPan(int dx, int dz) {
        this.panBy((double)(dx * 40) / this.zoom(), (double)(dz * 40) / this.zoom());
    }

    public void zoomBy(int delta, double anchorMouseX, double anchorMouseY) {
        int next = MathHelper.clamp((int)(this.zoomIndex + delta), (int)0, (int)(ZOOMS.length - 1));
        if (next != this.zoomIndex) {
            double anchorWX = this.screenToWorldX(anchorMouseX);
            double anchorWZ = this.screenToWorldZ(anchorMouseY);
            this.zoomIndex = next;
            this.centerX = anchorWX - (anchorMouseX - (double)(this.x0 + this.x1) / 2.0) / this.zoom();
            this.centerZ = anchorWZ - (anchorMouseY - (double)(this.y0 + this.y1) / 2.0) / this.zoom();
            this.clampCenter();
        }
    }

    public void render(DrawContext g, int mouseX, int mouseY, String selectedClaimId) {
        this.tiles.uploadIfDirty();
        this.clampCenter();
        this.requestVisibleTiles();
        g.fill(this.x0, this.y0, this.x1, this.y1, -16315620);
        g.enableScissor(this.x0, this.y0, this.x1, this.y1);
        this.drawVoidChecker(g);
        this.drawAtlas(g);
        this.drawGrid(g);
        this.drawWorldOutline(g, this.tiles.atlasMinBlockX(), this.tiles.atlasMinBlockZ(), this.tiles.atlasMinBlockX() + this.tiles.atlasSizePx(), this.tiles.atlasMinBlockZ() + this.tiles.atlasSizePx(), -13747610);
        this.drawClaimableRegion(g);
        this.drawClaims(g, mouseX, mouseY, selectedClaimId);
        this.drawSelection(g);
        this.drawPlayerMarker(g);
        g.disableScissor();
        g.drawBorder(this.x0 - 1, this.y0 - 1, this.x1 - this.x0 + 2, this.y1 - this.y0 + 2, -16447985);
        g.drawBorder(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0, -13747610);
    }

    private void requestVisibleTiles() {
        int visMinCx = MathHelper.floor((double)this.screenToWorldX(this.x0)) >> 4;
        int visMaxCx = MathHelper.floor((double)this.screenToWorldX(this.x1)) >> 4;
        int visMinCz = MathHelper.floor((double)this.screenToWorldZ(this.y0)) >> 4;
        int visMaxCz = MathHelper.floor((double)this.screenToWorldZ(this.y1)) >> 4;
        this.tiles.requestVisible(visMinCx - 2, visMinCz - 2, visMaxCx + 2, visMaxCz + 2, this.centerX / 16.0, this.centerZ / 16.0);
    }

    private void drawVoidChecker(DrawContext g) {
        int firstCx = MathHelper.floor((double)this.screenToWorldX(this.x0)) >> 4;
        int firstCz = MathHelper.floor((double)this.screenToWorldZ(this.y0)) >> 4;
        int lastCx = MathHelper.floor((double)this.screenToWorldX(this.x1)) >> 4;
        int lastCz = MathHelper.floor((double)this.screenToWorldZ(this.y1)) >> 4;
        for (int cx = firstCx; cx <= lastCx; ++cx) {
            int sx0 = (int)Math.round(this.worldToScreenX(cx << 4));
            int sx1 = (int)Math.round(this.worldToScreenX(cx + 1 << 4));
            for (int cz = firstCz; cz <= lastCz; ++cz) {
                if ((cx + cz & 1) == 0) continue;
                int sy0 = (int)Math.round(this.worldToScreenY(cz << 4));
                int sy1 = (int)Math.round(this.worldToScreenY(cz + 1 << 4));
                g.fill(sx0, sy0, sx1, sy1, -16052182);
            }
        }
    }

    private void drawAtlas(DrawContext g) {
        float screenX = (float)this.worldToScreenX(this.tiles.atlasMinBlockX());
        float screenY = (float)this.worldToScreenY(this.tiles.atlasMinBlockZ());
        int size = this.tiles.atlasSizePx();
        g.getMatrices().push();
        g.getMatrices().translate(screenX, screenY, 0.0f);
        g.getMatrices().scale((float)this.zoom(), (float)this.zoom(), 1.0f);
        g.drawTexture(this.tiles.location(), 0, 0, 0.0f, 0.0f, size, size, size, size);
        g.getMatrices().pop();
    }

    private void drawGrid(DrawContext g) {
        boolean minors = this.zoom() >= 1.0;
        int firstCx = MathHelper.floor((double)this.screenToWorldX(this.x0)) >> 4;
        int firstCz = MathHelper.floor((double)this.screenToWorldZ(this.y0)) >> 4;
        int cx = firstCx;
        while (true) {
            boolean major;
            int sx;
            if ((sx = (int)Math.round(this.worldToScreenX(cx << 4))) >= this.x1) {
                cx = firstCz;
                while (true) {
                    if ((sx = (int)Math.round(this.worldToScreenY(cx << 4))) >= this.y1) {
                        return;
                    }
                    if (sx >= this.y0) {
                        boolean bl = major = cx % 32 == 0;
                        if (major || minors) {
                            g.fill(this.x0, sx, this.x1, sx + 1, major ? 0x44FFFFFF : 0x22FFFFFF);
                        }
                    }
                    ++cx;
                }
            }
            if (sx >= this.x0) {
                boolean bl = major = cx % 32 == 0;
                if (major || minors) {
                    g.fill(sx, this.y0, sx + 1, this.y1, major ? 0x44FFFFFF : 0x22FFFFFF);
                }
            }
            ++cx;
        }
    }

    private void drawClaims(DrawContext g, int mouseX, int mouseY, String selectedClaimId) {
        MapClaimEntry hovered = this.hitClaim(mouseX, mouseY);
        for (MapClaimEntry claim : this.state.mapClaims()) {
            boolean isSelected = claim.getClaimId().equals(selectedClaimId);

            String relation = claim.getRelation() != null
                    ? claim.getRelation()
                    : "OTHER";

            int fill;
            int outline;

            switch (relation) {
                case "OWN" -> {
                    fill = 815615709;
                    outline = -6467875;
                }
                case "TRUSTED" -> {
                    fill = 809481910;
                    outline = -12601674;
                }
                case "ADMIN" -> {
                    fill = 819348816;
                    outline = -2734768;
                }
                default -> {
                    fill = 820017727;
                    outline = -5218256;
                }
            }

            this.drawWorldRect(
                    g,
                    claim.getMinX(),
                    claim.getMinZ(),
                    claim.getMaxX() + 1,
                    claim.getMaxZ() + 1,
                    fill,
                    outline
            );
            if (claim == hovered) {
                this.drawWorldFill(g, claim.getMinX(), claim.getMinZ(), claim.getMaxX() + 1, claim.getMaxZ() + 1, 0x18FFFFFF);
            }
            if (!isSelected) continue;
            this.drawSelectedTicks(g, claim);
            this.drawSubClaims(g, claim.getClaimId());
        }
        ChunkRect pending = this.state.pendingCreate();
        if (pending != null) {
            int alpha = 128 + (int)(96.0 * Math.sin((double)System.currentTimeMillis() / 180.0));
            int color = MathHelper.clamp((int)alpha, (int)64, (int)255) << 24 | 0xE0AAFF;
            this.drawWorldOutline(g, pending.getMinCx() << 4, pending.getMinCz() << 4, pending.getMaxCx() + 1 << 4, pending.getMaxCz() + 1 << 4, color);
        }
    }

    private void drawSubClaims(DrawContext g, String claimId) {
        ClaimDetailEntry detail = this.state.claimById(claimId);
        if (detail != null && detail.getSubClaims() != null) {
            for (SubClaimEntry sub : detail.getSubClaims()) {
                if (sub == null || sub.getBox() == null) continue;
                BoxInfo box = sub.getBox();
                this.drawWorldOutline(g, box.getMinX(), box.getMinZ(), box.getMaxX() + 1, box.getMaxZ() + 1, -4668724);
            }
        }
    }

    private void drawSelectedTicks(DrawContext g, MapClaimEntry claim) {
        this.drawCornerTicks(g, (int)Math.round(this.worldToScreenX(claim.getMinX())), (int)Math.round(this.worldToScreenY(claim.getMinZ())), (int)Math.round(this.worldToScreenX(claim.getMaxX() + 1)), (int)Math.round(this.worldToScreenY(claim.getMaxZ() + 1)));
    }

    private void drawCornerTicks(DrawContext g, int sx0, int sy0, int sx1, int sy1) {
        int t = 4;
        int c = -1;
        g.fill(sx0 - 1, sy0 - 1, sx0 + t, sy0, c);
        g.fill(sx0 - 1, sy0 - 1, sx0, sy0 + t, c);
        g.fill(sx1 - t, sy0 - 1, sx1 + 1, sy0, c);
        g.fill(sx1, sy0 - 1, sx1 + 1, sy0 + t, c);
        g.fill(sx0 - 1, sy1, sx0 + t, sy1 + 1, c);
        g.fill(sx0 - 1, sy1 - t, sx0, sy1, c);
        g.fill(sx1 - t, sy1, sx1 + 1, sy1 + 1, c);
        g.fill(sx1, sy1 - t, sx1 + 1, sy1, c);
    }

    private void drawSelection(DrawContext g) {
        if (this.selection.isActive()) {
            ChunkRect rect = this.selection.toRect();
            boolean valid = this.selectionIssue() == SelectionIssue.NONE;
            int bx0 = rect.getMinCx() << 4;
            int bz0 = rect.getMinCz() << 4;
            int bx1 = rect.getMaxCx() + 1 << 4;
            int bz1 = rect.getMaxCz() + 1 << 4;
            this.drawWorldRect(g, bx0, bz0, bx1, bz1, valid ? 1212262495 : 1222002000, valid ? -12474273 : -2734768);
            this.drawCornerTicks(g, (int)Math.round(this.worldToScreenX(bx0)), (int)Math.round(this.worldToScreenY(bz0)), (int)Math.round(this.worldToScreenX(bx1)), (int)Math.round(this.worldToScreenY(bz1)));
        }
    }

    private void drawClaimableRegion(DrawContext g) {
        if (!this.selection.isResize()) {
            int d = this.state.maxClaimDistanceChunks;
            int pcx = this.state.playerX >> 4;
            int pcz = this.state.playerZ >> 4;
            this.drawWorldOutline(g, pcx - d << 4, pcz - d << 4, pcx + d + 1 << 4, pcz + d + 1 << 4, 1728045194);
        }
    }

    private void drawPlayerMarker(DrawContext g) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            float sx = (float)this.worldToScreenX(player.getX());
            float sy = (float)this.worldToScreenY(player.getZ());
            int r = 5 + (int)Math.round(2.0 * Math.sin((double)System.currentTimeMillis() / 300.0));
            g.drawBorder((int)sx - r, (int)sy - r, 2 * r, 2 * r, 0x60FFFFFF);
            g.getMatrices().push();
            g.getMatrices().translate(sx, sy, 0.0f);
            g.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.getYaw() + 180.0f));
            g.fill(-1, -4, 2, -2, -16777216);
            g.fill(-2, -2, 3, 0, -16777216);
            g.fill(-3, 0, 4, 2, -16777216);
            g.fill(0, -3, 1, -2, -1);
            g.fill(-1, -2, 2, -1, -1);
            g.fill(-2, -1, 3, 1, -1);
            g.getMatrices().pop();
        }
    }

    private void drawWorldRect(DrawContext g, int wx0, int wz0, int wx1, int wz1, int fill, int outline) {
        this.drawWorldFill(g, wx0, wz0, wx1, wz1, fill);
        this.drawWorldOutline(g, wx0, wz0, wx1, wz1, outline);
    }

    private void drawWorldFill(DrawContext g, int wx0, int wz0, int wx1, int wz1, int color) {
        int sx0 = (int)Math.round(this.worldToScreenX(wx0));
        int sy0 = (int)Math.round(this.worldToScreenY(wz0));
        int sx1 = (int)Math.round(this.worldToScreenX(wx1));
        int sy1 = (int)Math.round(this.worldToScreenY(wz1));
        g.fill(sx0, sy0, sx1, sy1, color);
    }

    private void drawWorldOutline(DrawContext g, int wx0, int wz0, int wx1, int wz1, int color) {
        int sx0 = (int)Math.round(this.worldToScreenX(wx0));
        int sy0 = (int)Math.round(this.worldToScreenY(wz0));
        int sx1 = (int)Math.round(this.worldToScreenX(wx1));
        int sy1 = (int)Math.round(this.worldToScreenY(wz1));
        g.fill(sx0, sy0, sx1, sy0 + 1, color);
        g.fill(sx0, sy1 - 1, sx1, sy1, color);
        g.fill(sx0, sy0, sx0 + 1, sy1, color);
        g.fill(sx1 - 1, sy0, sx1, sy1, color);
    }

    public MapClaimEntry hitClaim(double mouseX, double mouseY) {
        if (!this.contains(mouseX, mouseY)) {
            return null;
        }
        int wx = this.cursorBlockX(mouseX);
        int wz = this.cursorBlockZ(mouseY);
        MapClaimEntry hit = null;
        for (MapClaimEntry claim : this.state.mapClaims()) {
            if (wx < claim.getMinX() || wx > claim.getMaxX() || wz < claim.getMinZ() || wz > claim.getMaxZ() || hit != null && !"OWN".equals(claim.getRelation())) continue;
            hit = claim;
        }
        return hit;
    }

    public SelectionIssue selectionIssue() {
        if (!this.selection.isActive()) {
            return SelectionIssue.NONE;
        }
        ChunkRect rect = this.selection.toRect();
        if (this.selection.widthChunks() <= this.state.maxMapClaimChunksPerSide && this.selection.depthChunks() <= this.state.maxMapClaimChunksPerSide) {
            ClaimDetailEntry current;
            if (!this.selection.isResize()) {
                int pcx = this.state.playerX >> 4;
                int pcz = this.state.playerZ >> 4;
                int dist = Math.max(Math.max(Math.abs(rect.getMinCx() - pcx), Math.abs(rect.getMinCz() - pcz)), Math.max(Math.abs(rect.getMaxCx() - pcx), Math.abs(rect.getMaxCz() - pcz)));
                if (dist > this.state.maxClaimDistanceChunks) {
                    return SelectionIssue.TOO_FAR;
                }
            }
            int bx0 = rect.getMinCx() << 4;
            int bz0 = rect.getMinCz() << 4;
            int bx1 = (rect.getMaxCx() + 1 << 4) - 1;
            int bz1 = (rect.getMaxCz() + 1 << 4) - 1;
            for (MapClaimEntry claim : this.state.mapClaims()) {
                if (this.selection.isResize() && claim.getClaimId().equals(this.selection.resizingClaimId()) || bx0 > claim.getMaxX() || bx1 < claim.getMinX() || bz0 > claim.getMaxZ() || bz1 < claim.getMinZ()) continue;
                return SelectionIssue.OVERLAP;
            }
            int cost = this.selection.areaBlocks();
            if (this.selection.isResize() && (current = this.state.claimById(this.selection.resizingClaimId())) != null) {
                cost -= current.getArea();
            }
            if (cost > this.state.effectiveRemaining()) {
                return SelectionIssue.TOO_EXPENSIVE;
            }
            return SelectionIssue.NONE;
        }
        return SelectionIssue.TOO_LARGE;
    }

    public int selectionCost() {
        ClaimDetailEntry current;
        int cost = this.selection.areaBlocks();
        if (this.selection.isResize() && (current = this.state.claimById(this.selection.resizingClaimId())) != null) {
            cost -= current.getArea();
        }
        return cost;
    }

    private boolean readOnly() {
        return this.state.adminTargetName != null;
    }

    public void mousePressed(double mouseX, double mouseY, int button, boolean shiftDown, String selectedClaimId) {
        this.pressButton = button;
        this.pressX = this.lastMouseX = mouseX;
        this.pressY = this.lastMouseY = mouseY;
        this.draggingPan = false;
        this.draggingSelect = false;
        if (button == 0 && !this.readOnly()) {
            if (this.grabCornerAt(mouseX, mouseY, selectedClaimId)) {
                this.draggingSelect = true;
            } else if (shiftDown) {
                int cx = this.cursorBlockX(mouseX) >> 4;
                int cz = this.cursorBlockZ(mouseY) >> 4;
                if (this.selection.isActive()) {
                    this.selection.drag(cx, cz);
                } else {
                    this.selection.start(cx, cz);
                }
                this.draggingSelect = true;
            }
        }
    }

    private boolean grabCornerAt(double mouseX, double mouseY, String selectedClaimId) {
        if (this.selection.isActive()) {
            ChunkRect r = this.selection.toRect();
            int[] corner = this.cornerChunkAt(mouseX, mouseY, r.getMinCx(), r.getMinCz(), r.getMaxCx(), r.getMaxCz());
            if (corner != null) {
                this.selection.grabCorner(corner[0], corner[1]);
                return true;
            }
            return false;
        }
        if (selectedClaimId == null) {
            return false;
        }
        ClaimDetailEntry detail = this.state.claimById(selectedClaimId);
        if (detail != null && detail.isOwner() && detail.getLocal() && !detail.is3D() && this.state.isOnMap(selectedClaimId)) {
            BoxInfo box = detail.getBox();
            int[] corner = this.cornerAt(mouseX, mouseY, box.getMinX(), box.getMinZ(), box.getMaxX() + 1, box.getMaxZ() + 1, box.getMinX() >> 4, box.getMinZ() >> 4, box.getMaxX() >> 4, box.getMaxZ() >> 4);
            if (corner == null) {
                return false;
            }
            this.selection.startResize(selectedClaimId, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
            this.selection.grabCorner(corner[0], corner[1]);
            return true;
        }
        return false;
    }

    private int[] cornerChunkAt(double mouseX, double mouseY, int minCx, int minCz, int maxCx, int maxCz) {
        return this.cornerAt(mouseX, mouseY, minCx << 4, minCz << 4, maxCx + 1 << 4, maxCz + 1 << 4, minCx, minCz, maxCx, maxCz);
    }

    private int[] cornerAt(double mouseX, double mouseY, int leftBlock, int topBlock, int rightBlock, int bottomBlock, int minCx, int minCz, int maxCx, int maxCz) {
        double left = this.worldToScreenX(leftBlock);
        double right = this.worldToScreenX(rightBlock);
        double top = this.worldToScreenY(topBlock);
        double bottom = this.worldToScreenY(bottomBlock);
        double[][] corners = new double[][]{{left, top}, {right, top}, {left, bottom}, {right, bottom}};
        int[][] chunks = new int[][]{{minCx, minCz}, {maxCx, minCz}, {minCx, maxCz}, {maxCx, maxCz}};
        for (int i = 0; i < 4; ++i) {
            if (!(Math.abs(mouseX - corners[i][0]) <= 7.0) || !(Math.abs(mouseY - corners[i][1]) <= 7.0)) continue;
            return chunks[i];
        }
        return null;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (this.pressButton != button) {
            return false;
        }
        if (this.draggingSelect) {
            this.selection.drag(this.cursorBlockX(mouseX) >> 4, this.cursorBlockZ(mouseY) >> 4);
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        if (!this.draggingPan && Math.hypot(mouseX - this.pressX, mouseY - this.pressY) < 4.0) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        this.draggingPan = true;
        this.centerX -= (mouseX - this.lastMouseX) / this.zoom();
        this.centerZ -= (mouseY - this.lastMouseY) / this.zoom();
        this.clampCenter();
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        return true;
    }

    public ClickResult mouseReleased(double mouseX, double mouseY, int button) {
        if (button != this.pressButton) {
            return null;
        }
        boolean wasClick = !this.draggingPan && !this.draggingSelect && this.contains(mouseX, mouseY);
        this.pressButton = -1;
        this.draggingPan = false;
        this.draggingSelect = false;
        if (button == 0 && wasClick) {
            MapClaimEntry hit = this.hitClaim(mouseX, mouseY);
            if (hit != null && !this.selection.isResize()) {
                this.selection.clear();
                return new ClickResult(hit, false);
            }
            if (this.selection.isResize()) {
                return null;
            }
            if (this.readOnly()) {
                return null;
            }
            this.selection.start(this.cursorBlockX(mouseX) >> 4, this.cursorBlockZ(mouseY) >> 4);
            return new ClickResult(null, true);
        }
        return null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        this.zoomBy((int)Math.signum(scrollY), mouseX, mouseY);
        return true;
    }

    @Environment(EnvType.CLIENT)
    public enum SelectionIssue {
        NONE,
        OVERLAP,
        TOO_EXPENSIVE,
        TOO_LARGE,
        TOO_FAR
    }

    @Environment(EnvType.CLIENT)
    public record ClickResult(MapClaimEntry claim, boolean startedSelection) {
    }
}
