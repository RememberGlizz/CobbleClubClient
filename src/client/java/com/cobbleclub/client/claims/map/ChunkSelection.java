package com.cobbleclub.client.claims.map;

import com.cobbleclub.clubhouse.claims.protocol.ChunkRect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ChunkSelection {
   private int anchorCx;
   private int anchorCz;
   private int extentCx;
   private int extentCz;
   private boolean active;
   private String resizingClaimId;

   public void start(int cx, int cz) {
      this.anchorCx = this.extentCx = cx;
      this.anchorCz = this.extentCz = cz;
      this.active = true;
      this.resizingClaimId = null;
   }

   public void startResize(String claimId, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
      this.anchorCx = minBlockX >> 4;
      this.anchorCz = minBlockZ >> 4;
      this.extentCx = maxBlockX >> 4;
      this.extentCz = maxBlockZ >> 4;
      this.active = true;
      this.resizingClaimId = claimId;
   }

   public void drag(int cx, int cz) {
      if (this.active) {
         this.extentCx = cx;
         this.extentCz = cz;
      }
   }

   public void grabCorner(int cx, int cz) {
      if (this.active) {
         ChunkRect r = this.toRect();
         this.anchorCx = cx == r.getMinCx() ? r.getMaxCx() : r.getMinCx();
         this.anchorCz = cz == r.getMinCz() ? r.getMaxCz() : r.getMinCz();
         this.extentCx = cx;
         this.extentCz = cz;
      }
   }

   public void clear() {
      this.active = false;
      this.resizingClaimId = null;
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isResize() {
      return this.resizingClaimId != null;
   }

   public String resizingClaimId() {
      return this.resizingClaimId;
   }

   public ChunkRect toRect() {
      return new ChunkRect(Math.min(this.anchorCx, this.extentCx), Math.min(this.anchorCz, this.extentCz), Math.max(this.anchorCx, this.extentCx), Math.max(this.anchorCz, this.extentCz));
   }

   public int widthChunks() {
      return Math.abs(this.extentCx - this.anchorCx) + 1;
   }

   public int depthChunks() {
      return Math.abs(this.extentCz - this.anchorCz) + 1;
   }

   public int areaBlocks() {
      return this.widthChunks() * this.depthChunks() * 256;
   }
}
