package com.cobbleclub.client.claims.map;

import com.cobbleclub.client.claims.ClaimsState;
import com.cobbleclub.clubhouse.claims.protocol.BoxInfo;
import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import com.cobbleclub.clubhouse.claims.protocol.ClaimDetailEntry;
import com.cobbleclub.clubhouse.claims.protocol.MapClaimEntry;
import com.cobbleclub.clubhouse.claims.protocol.SubClaimEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class MapView {
   private static final double[] ZOOMS = new double[]{(double)0.5F, (double)1.0F, (double)2.0F, (double)4.0F};
   private static final int GRID_LINE = 587202559;
   private static final int GRID_LINE_MAJOR = 1157627903;
   private static final int OWN_FILL = 815615709;
   private static final int OWN_OUTLINE = -6467875;
   private static final int TRUSTED_FILL = 809481910;
   private static final int TRUSTED_OUTLINE = -12601674;
   private static final int OTHER_FILL = 820017727;
   private static final int OTHER_OUTLINE = -5218256;
   private static final int ADMIN_FILL = 819348816;
   private static final int ADMIN_OUTLINE = -2734768;
   private static final int SUB_OUTLINE = -4668724;
   private static final int HOVER_BOOST = 419430399;
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
   private static final double DRAG_THRESHOLD_PX = (double)4.0F;
   private int pressButton = -1;
   private double pressX;
   private double pressY;
   private boolean draggingPan;
   private boolean draggingSelect;
   private double lastMouseX;
   private double lastMouseY;
   private static final double CORNER_GRAB_PX = (double)7.0F;

   public MapView(ClaimsState state, MapTileCache tiles) {
      this.state = state;
      this.tiles = tiles;
      this.centerX = (double)state.playerX;
      this.centerZ = (double)state.playerZ;
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
      return (double)(this.x0 + this.x1) / (double)2.0F + (wx - this.centerX) * this.zoom();
   }

   public double worldToScreenY(double wz) {
      return (double)(this.y0 + this.y1) / (double)2.0F + (wz - this.centerZ) * this.zoom();
   }

   public double screenToWorldX(double sx) {
      return this.centerX + (sx - (double)(this.x0 + this.x1) / (double)2.0F) / this.zoom();
   }

   public double screenToWorldZ(double sy) {
      return this.centerZ + (sy - (double)(this.y0 + this.y1) / (double)2.0F) / this.zoom();
   }

   public int cursorBlockX(double mouseX) {
      return MathHelper.floor(this.screenToWorldX(mouseX));
   }

   public int cursorBlockZ(double mouseY) {
      return MathHelper.floor(this.screenToWorldZ(mouseY));
   }

   private void clampCenter() {
      this.centerX = clampAxis(this.centerX, this.tiles.atlasMinBlockX(), this.tiles.atlasMinBlockX() + this.tiles.atlasSizePx(), (double)(this.x1 - this.x0) / this.zoom());
      this.centerZ = clampAxis(this.centerZ, this.tiles.atlasMinBlockZ(), this.tiles.atlasMinBlockZ() + this.tiles.atlasSizePx(), (double)(this.y1 - this.y0) / this.zoom());
   }

   private static double clampAxis(double center, int min, int max, double viewSpanBlocks) {
      double half = viewSpanBlocks / (double)2.0F;
      return viewSpanBlocks >= (double)(max - min) ? (double)(min + max) / (double)2.0F : MathHelper.clamp(center, (double)min + half, (double)max - half);
   }

   public void centerOnPlayer() {
      PlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null) {
         this.centerOn(player.getX(), player.getZ());
      } else {
         this.centerOn((double)this.state.playerX, (double)this.state.playerZ);
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
      int next = MathHelper.clamp(this.zoomIndex + delta, 0, ZOOMS.length - 1);
      if (next != this.zoomIndex) {
         double anchorWX = this.screenToWorldX(anchorMouseX);
         double anchorWZ = this.screenToWorldZ(anchorMouseY);
         this.zoomIndex = next;
         this.centerX = anchorWX - (anchorMouseX - (double)(this.x0 + this.x1) / (double)2.0F) / this.zoom();
         this.centerZ = anchorWZ - (anchorMouseY - (double)(this.y0 + this.y1) / (double)2.0F) / this.zoom();
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
      int visMinCx = MathHelper.floor(this.screenToWorldX((double)this.x0)) >> 4;
      int visMaxCx = MathHelper.floor(this.screenToWorldX((double)this.x1)) >> 4;
      int visMinCz = MathHelper.floor(this.screenToWorldZ((double)this.y0)) >> 4;
      int visMaxCz = MathHelper.floor(this.screenToWorldZ((double)this.y1)) >> 4;
      this.tiles.requestVisible(visMinCx - 2, visMinCz - 2, visMaxCx + 2, visMaxCz + 2, this.centerX / (double)16.0F, this.centerZ / (double)16.0F);
   }

   private void drawVoidChecker(DrawContext g) {
      int firstCx = MathHelper.floor(this.screenToWorldX((double)this.x0)) >> 4;
      int firstCz = MathHelper.floor(this.screenToWorldZ((double)this.y0)) >> 4;
      int lastCx = MathHelper.floor(this.screenToWorldX((double)this.x1)) >> 4;
      int lastCz = MathHelper.floor(this.screenToWorldZ((double)this.y1)) >> 4;

      for(int cx = firstCx; cx <= lastCx; ++cx) {
         int sx0 = (int)Math.round(this.worldToScreenX((double)(cx << 4)));
         int sx1 = (int)Math.round(this.worldToScreenX((double)(cx + 1 << 4)));

         for(int cz = firstCz; cz <= lastCz; ++cz) {
            if ((cx + cz & 1) != 0) {
               int sy0 = (int)Math.round(this.worldToScreenY((double)(cz << 4)));
               int sy1 = (int)Math.round(this.worldToScreenY((double)(cz + 1 << 4)));
               g.fill(sx0, sy0, sx1, sy1, -16052182);
            }
         }
      }

   }

   private void drawAtlas(DrawContext g) {
      float screenX = (float)this.worldToScreenX((double)this.tiles.atlasMinBlockX());
      float screenY = (float)this.worldToScreenY((double)this.tiles.atlasMinBlockZ());
      int size = this.tiles.atlasSizePx();
      g.getMatrices().push();
      g.getMatrices().translate(screenX, screenY, 0.0F);
      g.getMatrices().scale((float)this.zoom(), (float)this.zoom(), 1.0F);
      g.drawTexture(this.tiles.location(), 0, 0, 0.0F, 0.0F, size, size, size, size);
      g.getMatrices().pop();
   }

   private void drawGrid(DrawContext g) {
      boolean minors = this.zoom() >= (double)1.0F;
      int firstCx = MathHelper.floor(this.screenToWorldX((double)this.x0)) >> 4;
      int firstCz = MathHelper.floor(this.screenToWorldZ((double)this.y0)) >> 4;
      int cx = firstCx;

      while(true) {
         int sx = (int)Math.round(this.worldToScreenX((double)(cx << 4)));
         if (sx >= this.x1) {
            cx = firstCz;

            while(true) {
               sx = (int)Math.round(this.worldToScreenY((double)(cx << 4)));
               if (sx >= this.y1) {
                  return;
               }

               if (sx >= this.y0) {
                  boolean major = cx % 32 == 0;
                  if (major || minors) {
                     g.fill(this.x0, sx, this.x1, sx + 1, major ? 1157627903 : 587202559);
                  }
               }

               ++cx;
            }
         }

         if (sx >= this.x0) {
            boolean major = cx % 32 == 0;
            if (major || minors) {
               g.fill(sx, this.y0, sx + 1, this.y1, major ? 1157627903 : 587202559);
            }
         }

         ++cx;
      }
   }

   private void drawClaims(DrawContext g, int mouseX, int mouseY, String selectedClaimId) {
      MapClaimEntry hovered = this.hitClaim((double)mouseX, (double)mouseY);

      for(MapClaimEntry claim : this.state.mapClaims()) {
         boolean isSelected = claim.getClaimId().equals(selectedClaimId);
         int fill;
         int outline;
         switch (claim.getRelation() != null ? claim.getRelation() : "OTHER") {
            case "OWN":
               fill = 815615709;
               outline = -6467875;
               break;
            case "TRUSTED":
               fill = 809481910;
               outline = -12601674;
               break;
            case "ADMIN":
               fill = 819348816;
               outline = -2734768;
               break;
            default:
               fill = 820017727;
               outline = -5218256;
         }

         this.drawWorldRect(g, claim.getMinX(), claim.getMinZ(), claim.getMaxX() + 1, claim.getMaxZ() + 1, fill, outline);
         if (claim == hovered) {
            this.drawWorldFill(g, claim.getMinX(), claim.getMinZ(), claim.getMaxX() + 1, claim.getMaxZ() + 1, 419430399);
         }

         if (isSelected) {
            this.drawSelectedTicks(g, claim);
            this.drawSubClaims(g, claim.getClaimId());
         }
      }

      ChunkRect pending = this.state.pendingCreate();
      if (pending != null) {
         int alpha = 128 + (int)((double)96.0F * Math.sin((double)System.currentTimeMillis() / (double)180.0F));
         int color = MathHelper.clamp(alpha, 64, 255) << 24 | 14723839;
         this.drawWorldOutline(g, pending.getMinCx() << 4, pending.getMinCz() << 4, pending.getMaxCx() + 1 << 4, pending.getMaxCz() + 1 << 4, color);
      }

   }

   private void drawSubClaims(DrawContext g, String claimId) {
      ClaimDetailEntry detail = this.state.claimById(claimId);
      if (detail != null && detail.getSubClaims() != null) {
         for(SubClaimEntry sub : detail.getSubClaims()) {
            if (sub != null && sub.getBox() != null) {
               BoxInfo box = sub.getBox();
               this.drawWorldOutline(g, box.getMinX(), box.getMinZ(), box.getMaxX() + 1, box.getMaxZ() + 1, -4668724);
            }
         }

      }
   }

   private void drawSelectedTicks(DrawContext g, MapClaimEntry claim) {
      this.drawCornerTicks(g, (int)Math.round(this.worldToScreenX((double)claim.getMinX())), (int)Math.round(this.worldToScreenY((double)claim.getMinZ())), (int)Math.round(this.worldToScreenX((double)(claim.getMaxX() + 1))), (int)Math.round(this.worldToScreenY((double)(claim.getMaxZ() + 1))));
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
         boolean valid = this.selectionIssue() == MapView.SelectionIssue.NONE;
         int bx0 = rect.getMinCx() << 4;
         int bz0 = rect.getMinCz() << 4;
         int bx1 = rect.getMaxCx() + 1 << 4;
         int bz1 = rect.getMaxCz() + 1 << 4;
         this.drawWorldRect(g, bx0, bz0, bx1, bz1, valid ? 1212262495 : 1222002000, valid ? -12474273 : -2734768);
         this.drawCornerTicks(g, (int)Math.round(this.worldToScreenX((double)bx0)), (int)Math.round(this.worldToScreenY((double)bz0)), (int)Math.round(this.worldToScreenX((double)bx1)), (int)Math.round(this.worldToScreenY((double)bz1)));
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
         int r = 5 + (int)Math.round((double)2.0F * Math.sin((double)System.currentTimeMillis() / (double)300.0F));
         g.drawBorder((int)sx - r, (int)sy - r, 2 * r, 2 * r, 1627389951);
         g.getMatrices().push();
         g.getMatrices().translate(sx, sy, 0.0F);
         g.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.getYaw() + 180.0F));
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
      int sx0 = (int)Math.round(this.worldToScreenX((double)wx0));
      int sy0 = (int)Math.round(this.worldToScreenY((double)wz0));
      int sx1 = (int)Math.round(this.worldToScreenX((double)wx1));
      int sy1 = (int)Math.round(this.worldToScreenY((double)wz1));
      g.fill(sx0, sy0, sx1, sy1, color);
   }

   private void drawWorldOutline(DrawContext g, int wx0, int wz0, int wx1, int wz1, int color) {
      int sx0 = (int)Math.round(this.worldToScreenX((double)wx0));
      int sy0 = (int)Math.round(this.worldToScreenY((double)wz0));
      int sx1 = (int)Math.round(this.worldToScreenX((double)wx1));
      int sy1 = (int)Math.round(this.worldToScreenY((double)wz1));
      g.fill(sx0, sy0, sx1, sy0 + 1, color);
      g.fill(sx0, sy1 - 1, sx1, sy1, color);
      g.fill(sx0, sy0, sx0 + 1, sy1, color);
      g.fill(sx1 - 1, sy0, sx1, sy1, color);
   }

   public MapClaimEntry hitClaim(double mouseX, double mouseY) {
      if (!this.contains(mouseX, mouseY)) {
         return null;
      } else {
         int wx = this.cursorBlockX(mouseX);
         int wz = this.cursorBlockZ(mouseY);
         MapClaimEntry hit = null;

         for(MapClaimEntry claim : this.state.mapClaims()) {
            if (wx >= claim.getMinX() && wx <= claim.getMaxX() && wz >= claim.getMinZ() && wz <= claim.getMaxZ() && (hit == null || "OWN".equals(claim.getRelation()))) {
               hit = claim;
            }
         }

         return hit;
      }
   }

   public SelectionIssue selectionIssue() {
      if (!this.selection.isActive()) {
         return MapView.SelectionIssue.NONE;
      } else {
         ChunkRect rect = this.selection.toRect();
         if (this.selection.widthChunks() <= this.state.maxMapClaimChunksPerSide && this.selection.depthChunks() <= this.state.maxMapClaimChunksPerSide) {
            if (!this.selection.isResize()) {
               int pcx = this.state.playerX >> 4;
               int pcz = this.state.playerZ >> 4;
               int dist = Math.max(Math.max(Math.abs(rect.getMinCx() - pcx), Math.abs(rect.getMinCz() - pcz)), Math.max(Math.abs(rect.getMaxCx() - pcx), Math.abs(rect.getMaxCz() - pcz)));
               if (dist > this.state.maxClaimDistanceChunks) {
                  return MapView.SelectionIssue.TOO_FAR;
               }
            }

            int bx0 = rect.getMinCx() << 4;
            int bz0 = rect.getMinCz() << 4;
            int bx1 = (rect.getMaxCx() + 1 << 4) - 1;
            int bz1 = (rect.getMaxCz() + 1 << 4) - 1;

            for(MapClaimEntry claim : this.state.mapClaims()) {
               if ((!this.selection.isResize() || !claim.getClaimId().equals(this.selection.resizingClaimId())) && bx0 <= claim.getMaxX() && bx1 >= claim.getMinX() && bz0 <= claim.getMaxZ() && bz1 >= claim.getMinZ()) {
                  return MapView.SelectionIssue.OVERLAP;
               }
            }

            int cost = this.selection.areaBlocks();
            if (this.selection.isResize()) {
               ClaimDetailEntry current = this.state.claimById(this.selection.resizingClaimId());
               if (current != null) {
                  cost -= current.getArea();
               }
            }

            if (cost > this.state.effectiveRemaining()) {
               return MapView.SelectionIssue.TOO_EXPENSIVE;
            } else {
               return MapView.SelectionIssue.NONE;
            }
         } else {
            return MapView.SelectionIssue.TOO_LARGE;
         }
      }
   }

   public int selectionCost() {
      int cost = this.selection.areaBlocks();
      if (this.selection.isResize()) {
         ClaimDetailEntry current = this.state.claimById(this.selection.resizingClaimId());
         if (current != null) {
            cost -= current.getArea();
         }
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
         } else {
            if (shiftDown) {
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
   }

   private boolean grabCornerAt(double mouseX, double mouseY, String selectedClaimId) {
      if (this.selection.isActive()) {
         ChunkRect r = this.selection.toRect();
         int[] corner = this.cornerChunkAt(mouseX, mouseY, r.getMinCx(), r.getMinCz(), r.getMaxCx(), r.getMaxCz());
         if (corner != null) {
            this.selection.grabCorner(corner[0], corner[1]);
            return true;
         } else {
            return false;
         }
      } else if (selectedClaimId == null) {
         return false;
      } else {
         ClaimDetailEntry detail = this.state.claimById(selectedClaimId);
         if (detail != null && detail.isOwner() && detail.getLocal() && !detail.is3D() && this.state.isOnMap(selectedClaimId)) {
            BoxInfo box = detail.getBox();
            int[] corner = this.cornerAt(mouseX, mouseY, box.getMinX(), box.getMinZ(), box.getMaxX() + 1, box.getMaxZ() + 1, box.getMinX() >> 4, box.getMinZ() >> 4, box.getMaxX() >> 4, box.getMaxZ() >> 4);
            if (corner == null) {
               return false;
            } else {
               this.selection.startResize(selectedClaimId, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
               this.selection.grabCorner(corner[0], corner[1]);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private int[] cornerChunkAt(double mouseX, double mouseY, int minCx, int minCz, int maxCx, int maxCz) {
      return this.cornerAt(mouseX, mouseY, minCx << 4, minCz << 4, maxCx + 1 << 4, maxCz + 1 << 4, minCx, minCz, maxCx, maxCz);
   }

   private int[] cornerAt(double mouseX, double mouseY, int leftBlock, int topBlock, int rightBlock, int bottomBlock, int minCx, int minCz, int maxCx, int maxCz) {
      double left = this.worldToScreenX((double)leftBlock);
      double right = this.worldToScreenX((double)rightBlock);
      double top = this.worldToScreenY((double)topBlock);
      double bottom = this.worldToScreenY((double)bottomBlock);
      double[][] corners = new double[][]{{left, top}, {right, top}, {left, bottom}, {right, bottom}};
      int[][] chunks = new int[][]{{minCx, minCz}, {maxCx, minCz}, {minCx, maxCz}, {maxCx, maxCz}};

      for(int i = 0; i < 4; ++i) {
         if (Math.abs(mouseX - corners[i][0]) <= (double)7.0F && Math.abs(mouseY - corners[i][1]) <= (double)7.0F) {
            return chunks[i];
         }
      }

      return null;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button) {
      if (this.pressButton != button) {
         return false;
      } else if (this.draggingSelect) {
         this.selection.drag(this.cursorBlockX(mouseX) >> 4, this.cursorBlockZ(mouseY) >> 4);
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      } else if (!this.draggingPan && Math.hypot(mouseX - this.pressX, mouseY - this.pressY) < (double)4.0F) {
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      } else {
         this.draggingPan = true;
         this.centerX -= (mouseX - this.lastMouseX) / this.zoom();
         this.centerZ -= (mouseY - this.lastMouseY) / this.zoom();
         this.clampCenter();
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      }
   }

   public ClickResult mouseReleased(double mouseX, double mouseY, int button) {
      if (button != this.pressButton) {
         return null;
      } else {
         boolean wasClick = !this.draggingPan && !this.draggingSelect && this.contains(mouseX, mouseY);
         this.pressButton = -1;
         this.draggingPan = false;
         this.draggingSelect = false;
         if (button == 0 && wasClick) {
            MapClaimEntry hit = this.hitClaim(mouseX, mouseY);
            if (hit != null && !this.selection.isResize()) {
               this.selection.clear();
               return new ClickResult(hit, false);
            } else if (this.selection.isResize()) {
               return null;
            } else if (this.readOnly()) {
               return null;
            } else {
               this.selection.start(this.cursorBlockX(mouseX) >> 4, this.cursorBlockZ(mouseY) >> 4);
               return new ClickResult((MapClaimEntry)null, true);
            }
         } else {
            return null;
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      this.zoomBy((int)Math.signum(scrollY), mouseX, mouseY);
      return true;
   }

   @Environment(EnvType.CLIENT)
   public static enum SelectionIssue {
      NONE,
      OVERLAP,
      TOO_EXPENSIVE,
      TOO_LARGE,
      TOO_FAR;

      // $FF: synthetic method
      private static SelectionIssue[] $values() {
         return new SelectionIssue[]{NONE, OVERLAP, TOO_EXPENSIVE, TOO_LARGE, TOO_FAR};
      }
   }

   @Environment(EnvType.CLIENT)
   public static record ClickResult(MapClaimEntry claim, boolean startedSelection) {
   }
}
