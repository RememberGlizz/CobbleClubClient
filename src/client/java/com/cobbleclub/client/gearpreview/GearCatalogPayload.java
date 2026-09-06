package com.cobbleclub.client.gearpreview;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record GearCatalogPayload(List<GearCatalogSet> sets) implements CustomPayload {
   public static final CustomPayload.Id<GearCatalogPayload> TYPE = new CustomPayload.Id(Identifier.of("cobbleclub", "gear_catalog/v1"));
   public static final PacketCodec<RegistryByteBuf, GearCatalogPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> {
      buf.writeVarInt(payload.sets().size());

      for(GearCatalogSet set : payload.sets()) {
         buf.writeString(set.id());
         buf.writeString(set.displayName());
         buf.writeVarInt(set.items().size());

         for(ItemStack item : set.items()) {
            ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, item);
         }
      }

   }, (buf) -> {
      int setCount = buf.readVarInt();
      List<GearCatalogSet> sets = new ArrayList(setCount);

      for(int i = 0; i < setCount; ++i) {
         String id = buf.readString();
         String displayName = buf.readString();
         int itemCount = buf.readVarInt();
         List<ItemStack> items = new ArrayList(itemCount);

         for(int j = 0; j < itemCount; ++j) {
            items.add((ItemStack)ItemStack.OPTIONAL_PACKET_CODEC.decode(buf));
         }

         sets.add(new GearCatalogSet(id, displayName, items));
      }

      return new GearCatalogPayload(sets);
   });

   public CustomPayload.Id<? extends CustomPayload> getId() {
      return TYPE;
   }

   @Environment(EnvType.CLIENT)
   public static record GearCatalogSet(String id, String displayName, List<ItemStack> items) {
   }
}
