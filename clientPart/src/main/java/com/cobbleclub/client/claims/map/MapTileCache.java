package com.cobbleclub.client.claims.map;

import com.cobbleclub.client.claims.ClaimsNetworking;
import com.cobbleclub.clubhouse.claims.protocol.ClaimsMapTilesMsg;
import com.cobbleclub.clubhouse.claims.protocol.MapTileEntry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3620;

@Environment(EnvType.CLIENT)
public final class MapTileCache implements AutoCloseable {
   private static final int BACKGROUND = 0;
   private static final byte STATE_MISSING = 0;
   private static final byte STATE_REQUESTED = 1;
   private static final byte STATE_LOADED = 2;
   private static final byte STATE_UNLOADED = 3;
   private static final byte STATE_PENDING = 4;
   private static final long REQUEST_TIMEOUT_MS = 5000L;
   private static final long PENDING_RETRY_MS = 1500L;
   private static final long REQUEST_FLUSH_MS = 250L;
   private static final int MAX_CHUNKS_PER_REQUEST = 128;
   private final String dimension;
   private final int minCx;
   private final int minCz;
   private final int side;
   private final byte[] states;
   private final long[] stateTimes;
   private final class_1043 texture;
   private final class_2960 location;
   private boolean dirty;
   private long lastFlushMillis;

   public MapTileCache(String dimension, int originCx, int originCz, int radiusChunks) {
      this.dimension = dimension;
      this.minCx = originCx - radiusChunks;
      this.minCz = originCz - radiusChunks;
      this.side = radiusChunks * 2 + 1;
      this.states = new byte[this.side * this.side];
      this.stateTimes = new long[this.side * this.side];
      this.texture = new class_1043(this.side * 16, this.side * 16, false);
      class_1011 image = this.texture.method_4525();
      if (image != null) {
         image.method_4326(0, 0, this.side * 16, this.side * 16, 0);
      }

      this.dirty = true;
      this.location = class_310.method_1551().method_1531().method_4617("claims_map", this.texture);
   }

   public class_2960 location() {
      return this.location;
   }

   public int atlasSizePx() {
      return this.side * 16;
   }

   public int atlasMinBlockX() {
      return this.minCx << 4;
   }

   public int atlasMinBlockZ() {
      return this.minCz << 4;
   }

   public void accept(ClaimsMapTilesMsg msg) {
      if (this.dimension.equals(msg.getDimension())) {
         long now = System.currentTimeMillis();
         if (msg.getTiles() != null) {
            for(MapTileEntry tile : msg.getTiles()) {
               if (tile != null && tile.getData() != null) {
                  this.writeTile(tile.getCx(), tile.getCz(), tile.getData(), now);
               }
            }
         }

         if (msg.getPending() != null) {
            for(List<Integer> pair : msg.getPending()) {
               this.setState(pair, (byte)4, now);
            }
         }

         if (msg.getMissing() != null) {
            for(List<Integer> pair : msg.getMissing()) {
               this.setState(pair, (byte)3, now);
            }
         }

      }
   }

   private void setState(List<Integer> pair, byte state, long now) {
      if (pair != null && pair.size() == 2) {
         int index = this.indexOf((Integer)pair.get(0), (Integer)pair.get(1));
         if (index >= 0 && this.states[index] != 2) {
            this.states[index] = state;
            this.stateTimes[index] = now;
         }
      }
   }

   private void writeTile(int cx, int cz, String base64, long now) {
      int index = this.indexOf(cx, cz);
      if (index >= 0) {
         byte[] bytes;
         try {
            bytes = Base64.getDecoder().decode(base64);
         } catch (IllegalArgumentException var14) {
            return;
         }

         if (bytes.length == 256) {
            class_1011 image = this.texture.method_4525();
            if (image != null) {
               int px0 = (cx - this.minCx) * 16;
               int pz0 = (cz - this.minCz) * 16;

               for(int z = 0; z < 16; ++z) {
                  for(int x = 0; x < 16; ++x) {
                     int packed = bytes[x + z * 16] & 255;
                     image.method_4305(px0 + x, pz0 + z, packed == 0 ? 0 : class_3620.method_38480(packed));
                  }
               }

               this.states[index] = 2;
               this.stateTimes[index] = now;
               this.dirty = true;
            }
         }
      }
   }

   public void uploadIfDirty() {
      if (this.dirty) {
         this.dirty = false;
         this.texture.method_4524();
      }
   }

   public void requestVisible(int visMinCx, int visMinCz, int visMaxCx, int visMaxCz, double centerCx, double centerCz) {
      long now = System.currentTimeMillis();
      if (now - this.lastFlushMillis >= 250L) {
         this.lastFlushMillis = now;
         List<int[]> wanted = new ArrayList();

         for(int cz = Math.max(visMinCz, this.minCz); cz <= Math.min(visMaxCz, this.minCz + this.side - 1); ++cz) {
            for(int cx = Math.max(visMinCx, this.minCx); cx <= Math.min(visMaxCx, this.minCx + this.side - 1); ++cx) {
               int index = cx - this.minCx + (cz - this.minCz) * this.side;
               byte state = this.states[index];
               if (state != 2 && state != 3 && (state != 1 || now - this.stateTimes[index] >= 5000L) && (state != 4 || now - this.stateTimes[index] >= 1500L)) {
                  wanted.add(new int[]{cx, cz});
               }
            }
         }

         if (!wanted.isEmpty()) {
            wanted.sort(Comparator.comparingDouble((pairx) -> Math.max(Math.abs((double)pairx[0] - centerCx), Math.abs((double)pairx[1] - centerCz))));
            List<List<Integer>> request = new ArrayList();

            for(int i = 0; i < wanted.size() && i < 128; ++i) {
               int[] pair = (int[])wanted.get(i);
               request.add(List.of(pair[0], pair[1]));
               int index = pair[0] - this.minCx + (pair[1] - this.minCz) * this.side;
               this.states[index] = 1;
               this.stateTimes[index] = now;
            }

            ClaimsNetworking.sendMapRequest(this.dimension, request);
         }
      }
   }

   private int indexOf(int cx, int cz) {
      int tx = cx - this.minCx;
      int tz = cz - this.minCz;
      return tx >= 0 && tz >= 0 && tx < this.side && tz < this.side ? tx + tz * this.side : -1;
   }

   public void close() {
      class_310.method_1551().method_1531().method_4615(this.location);
   }
}
