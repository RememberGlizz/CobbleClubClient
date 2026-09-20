/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9139
 */
package com.cobbleclub.client.gearpreview;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

@Environment(value=EnvType.CLIENT)
public record GearCatalogPayload(List<GearCatalogSet> sets) implements CustomPayload
{
    public static final CustomPayload.Id<GearCatalogPayload> TYPE = new CustomPayload.Id(Identifier.of((String)"cobbleclub", (String)"gear_catalog/v1"));
    public static final PacketCodec<RegistryByteBuf, GearCatalogPayload> STREAM_CODEC = PacketCodec.ofStatic((buf, payload) -> {
        buf.writeVarInt(payload.sets().size());
        for (GearCatalogSet set : payload.sets()) {
            buf.writeString(set.id());
            buf.writeString(set.displayName());
            buf.writeVarInt(set.items().size());
            for (ItemStack item : set.items()) {
                ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, item);
            }
        }
    }, buf -> {
        int setCount = buf.readVarInt();
        ArrayList<GearCatalogSet> sets = new ArrayList<GearCatalogSet>(setCount);
        for (int i = 0; i < setCount; ++i) {
            String id = buf.readString();
            String displayName = buf.readString();
            int itemCount = buf.readVarInt();
            ArrayList<ItemStack> items = new ArrayList<ItemStack>(itemCount);
            for (int j = 0; j < itemCount; ++j) {
                items.add((ItemStack)ItemStack.OPTIONAL_PACKET_CODEC.decode(buf));
            }
            sets.add(new GearCatalogSet(id, displayName, items));
        }
        return new GearCatalogPayload(sets);
    });

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Environment(value=EnvType.CLIENT)
    public record GearCatalogSet(String id, String displayName, List<ItemStack> items) {
    }
}

