package com.cobbleclub.client.sizer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record LayerCodec(String pokemon, Map<String, Map<String, Settings>> size_config) {
   public static final Codec<LayerCodec> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.STRING.fieldOf("pokemon").forGetter(LayerCodec::pokemon), Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, LayerCodec.Settings.CODEC)).fieldOf("size_config").forGetter(LayerCodec::size_config)).apply(instance, LayerCodec::new));

   @Environment(EnvType.CLIENT)
   public static record Settings(List<Float> scale, List<Float> translate) {
      public static final Codec<Settings> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.list(Codec.FLOAT).optionalFieldOf("scale", List.of(1.0F, 1.0F, 1.0F)).forGetter(Settings::scale), Codec.list(Codec.FLOAT).optionalFieldOf("translate", List.of(0.0F, 0.0F, 0.0F)).forGetter(Settings::translate)).apply(instance, Settings::new));
   }
}
