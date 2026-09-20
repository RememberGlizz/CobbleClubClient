/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.cobblemon.mod.common.api.pokemon.PokemonSpecies
 *  com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt
 *  com.cobblemon.mod.common.client.gui.ProfileTransformType
 *  com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
 *  com.cobblemon.mod.common.client.render.models.blockbench.PosableState
 *  com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
 *  com.cobblemon.mod.common.entity.PoseType
 *  com.cobblemon.mod.common.item.PokemonItem
 *  com.cobblemon.mod.common.pokemon.RenderablePokemon
 *  com.cobblemon.mod.common.pokemon.Species
 *  kotlin.Metadata
 *  kotlin.collections.CollectionsKt
 *  kotlin.jvm.JvmStatic
 *  kotlin.jvm.internal.Intrinsics
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  org.jetbrains.annotations.NotNull
 *  org.joml.Quaternionf
 */
package com.cobbleclub.client.pokemonpreview;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.Intrinsics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000`\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J%\u0010\t\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\u00042\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006H\u0007\u00a2\u0006\u0004\b\t\u0010\nJ\u0017\u0010\f\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\u0004H\u0007\u00a2\u0006\u0004\b\f\u0010\rJu\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0016\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0005\u001a\u00020\u00042\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u00062\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u0014H\u0007\u00a2\u0006\u0004\b\u001c\u0010\u001dJ%\u0010 \u001a\u00020\u00182\u0006\u0010\u001f\u001a\u00020\u001e2\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006H\u0002\u00a2\u0006\u0004\b \u0010!R\u0014\u0010#\u001a\u00020\"8\u0002X\u0082T\u00a2\u0006\u0006\n\u0004\b#\u0010$R\u0014\u0010%\u001a\u00020\"8\u0002X\u0082T\u00a2\u0006\u0006\n\u0004\b%\u0010$R\u0016\u0010'\u001a\u00020&8\u0002@\u0002X\u0082\u000e\u00a2\u0006\u0006\n\u0004\b'\u0010(R\u0016\u0010)\u001a\u00020\u00048\u0002@\u0002X\u0082\u000e\u00a2\u0006\u0006\n\u0004\b)\u0010*R\u0016\u0010+\u001a\u00020\u00188\u0002@\u0002X\u0082\u000e\u00a2\u0006\u0006\n\u0004\b+\u0010,\u00a8\u0006-"}, d2={"Lcom/cobbleclub/client/pokemonpreview/PokemonPreviewRenderer;", "", "<init>", "()V", "", "species", "", "aspects", "Lnet/minecraft/class_1799;", "modelStack", "(Ljava/lang/String;Ljava/util/List;)Lnet/minecraft/class_1799;", "", "dexNumber", "(Ljava/lang/String;)I", "Lnet/minecraft/class_332;", "g", "x0", "y0", "x1", "y1", "", "yaw", "pitch", "zoom", "", "idleFly", "partialTicks", "", "render", "(Lnet/minecraft/class_332;IIIIFFFLjava/lang/String;Ljava/util/List;ZF)V", "Lnet/minecraft/class_2960;", "id", "hasHoverPose", "(Lnet/minecraft/class_2960;Ljava/util/List;)Z", "", "SCALE_FRAC", "D", "VISUAL_CENTER_K", "Lcom/cobblemon/mod/common/client/render/models/blockbench/FloatingState;", "state", "Lcom/cobblemon/mod/common/client/render/models/blockbench/FloatingState;", "lastKey", "Ljava/lang/String;", "hoverPose", "Z", "clubhouse-client-cobbleclub_client"})
@Environment(value=EnvType.CLIENT)
public final class PokemonPreviewRenderer {
    @NotNull
    public static final PokemonPreviewRenderer INSTANCE = new PokemonPreviewRenderer();
    private static final double SCALE_FRAC = 0.021;
    private static final double VISUAL_CENTER_K = 21.5;
    @NotNull
    private static FloatingState state = new FloatingState();
    @NotNull
    private static String lastKey = "";
    private static boolean hoverPose;

    private PokemonPreviewRenderer() {
    }

    private static Identifier speciesId(String species) {
        if (species == null || species.isBlank()) {
            return null;
        }
        Object normalized = species.indexOf(58) >= 0 ? species : "cobblemon:" + species;
        return Identifier.tryParse((String)normalized);
    }

    @JvmStatic
    @NotNull
    public static final ItemStack modelStack(@NotNull String species, @NotNull List<String> aspects) {
        Intrinsics.checkNotNullParameter((Object)species, (String)"species");
        Intrinsics.checkNotNullParameter(aspects, (String)"aspects");
        Identifier var10000 = PokemonPreviewRenderer.speciesId(species);
        if (var10000 == null) {
            ItemStack var6 = ItemStack.EMPTY;
            Intrinsics.checkNotNullExpressionValue((Object)var6, (String)"EMPTY");
            return var6;
        }
        Identifier id = var10000;
        Species var4 = PokemonSpecies.getByIdentifier((Identifier)id);
        if (var4 == null) {
            ItemStack var5 = ItemStack.EMPTY;
            Intrinsics.checkNotNullExpressionValue((Object)var5, (String)"EMPTY");
            return var5;
        }
        Species sp = var4;
        Set safeAspects = aspects.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return PokemonItem.Companion.from(sp, safeAspects, 1, null);
    }

    @JvmStatic
    public static final int dexNumber(@NotNull String species) {
        Intrinsics.checkNotNullParameter((Object)species, (String)"species");
        Identifier var10000 = PokemonPreviewRenderer.speciesId(species);
        if (var10000 == null) {
            return Integer.MAX_VALUE;
        }
        Identifier id = var10000;
        Species var2 = PokemonSpecies.getByIdentifier((Identifier)id);
        return var2 != null ? var2.getNationalPokedexNumber() : Integer.MAX_VALUE;
    }

    @JvmStatic
    public static final void render(@NotNull DrawContext g, int x0, int y0, int x1, int y1, float yaw, float pitch, float zoom, @NotNull String species, @NotNull List<String> aspects, boolean idleFly, float partialTicks) {
        Identifier id;
        Species var22;
        Intrinsics.checkNotNullParameter((Object)g, (String)"g");
        Intrinsics.checkNotNullParameter((Object)species, (String)"species");
        Intrinsics.checkNotNullParameter(aspects, (String)"aspects");
        Identifier var10000 = PokemonPreviewRenderer.speciesId(species);
        if (var10000 != null && (var22 = PokemonSpecies.getByIdentifier((Identifier)(id = var10000))) != null) {
            Species sp = var22;
            List<String> safeAspects = aspects.stream().filter(Objects::nonNull).toList();
            String key = species + "|" + safeAspects.stream().sorted().collect(Collectors.joining(","));
            if (!Intrinsics.areEqual((Object)key, (Object)lastKey)) {
                state = new FloatingState();
                lastKey = key;
                hoverPose = INSTANCE.hasHoverPose(id, safeAspects);
            }
            double paneH = y1 - y0;
            float outerScale = (float)(paneH * 0.021 * (double)zoom);
            double anchorY = (double)(y0 + y1) / 2.0 - (double)outerScale * 21.5;
            g.enableScissor(x0, y0, x1, y1);
            g.getMatrices().push();
            g.getMatrices().translate((double)(x0 + x1) / 2.0, anchorY, 1000.0);
            g.getMatrices().scale(outerScale, outerScale, outerScale);
            Quaternionf rotation = new Quaternionf().rotationXYZ((float)Math.toRadians(pitch), (float)Math.toRadians(yaw), 0.0f);
            RenderablePokemon var23 = new RenderablePokemon(sp, Set.copyOf(safeAspects), ItemStack.EMPTY);
            MatrixStack var10001 = g.getMatrices();
            Intrinsics.checkNotNullExpressionValue((Object)var10001, (String)"pose(...)");
            Intrinsics.checkNotNull((Object)rotation);
            PokemonGuiUtilsKt.drawProfilePokemon((RenderablePokemon)var23, (MatrixStack)var10001, (Quaternionf)rotation, (PoseType)(idleFly && hoverPose ? PoseType.HOVER : PoseType.PROFILE), (PosableState)state, (float)partialTicks, (float)20.0f, (ProfileTransformType)ProfileTransformType.PROFILE, (boolean)false, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)0.0f, (int)13);
            g.draw();
            g.getMatrices().pop();
            g.disableScissor();
        }
    }

    private final boolean hasHoverPose(Identifier id, List<String> aspects) {
        boolean var3;
        try {
            state.setCurrentAspects(CollectionsKt.toSet(aspects));
            var3 = VaryingModelRepository.INSTANCE.getPoser(id, (PosableState)state).getPose(PoseType.HOVER) != null;
        }
        catch (Exception var5) {
            var3 = false;
        }
        return var3;
    }
}

