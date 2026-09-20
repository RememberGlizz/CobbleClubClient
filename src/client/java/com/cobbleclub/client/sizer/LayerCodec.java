/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cobbleclub.client.sizer;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record LayerCodec(String pokemon, Map<String, Map<String, Settings>> size_config) {
    public static final Codec<LayerCodec> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.STRING.fieldOf("pokemon").forGetter(LayerCodec::pokemon), Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Settings.CODEC)).fieldOf("size_config").forGetter(LayerCodec::size_config)).apply(instance, LayerCodec::new));

    @Environment(value=EnvType.CLIENT)
    public record Settings(List<Float> scale, List<Float> translate) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.list(Codec.FLOAT).optionalFieldOf("scale", List.of(Float.valueOf(1.0f), Float.valueOf(1.0f), Float.valueOf(1.0f))).forGetter(Settings::scale), Codec.list(Codec.FLOAT).optionalFieldOf("translate", List.of(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(0.0f))).forGetter(Settings::translate)).apply(instance, Settings::new));
    }
}

