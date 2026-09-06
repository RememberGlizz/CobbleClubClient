package com.cobbleclub.client;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.cobblemon.mod.common.util.MoLangExtensionsKt;
import java.util.Map;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

@Metadata(
   mv = {2, 2, 0},
   k = 1,
   xi = 48,
   d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J%\u0010\n\u001a\u00020\t2\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u0007¢\u0006\u0004\b\n\u0010\u000b¨\u0006\f"},
   d2 = {"Lcom/cobbleclub/client/KotlinHelperFabric;", "", "<init>", "()V", "", "particleId", "locator", "Lcom/bedrockk/molang/runtime/MoLangRuntime;", "runtime", "", "playParticleEffect", "(Ljava/lang/String;Ljava/lang/String;Lcom/bedrockk/molang/runtime/MoLangRuntime;)V", "clubhouse-client-cobbleclub_client"}
)
@Environment(EnvType.CLIENT)
public final class KotlinHelperFabric {
   @NotNull
   public static final KotlinHelperFabric INSTANCE = new KotlinHelperFabric();

   private KotlinHelperFabric() {
   }

   public final void playParticleEffect(@NotNull String particleId, @NotNull String locator, @NotNull MoLangRuntime runtime) {
      Intrinsics.checkNotNullParameter(particleId, "particleId");
      Intrinsics.checkNotNullParameter(locator, "locator");
      Intrinsics.checkNotNullParameter(runtime, "runtime");
      MoLangExtensionsKt.resolve(runtime, MoLangExtensionsKt.asExpressionLike("q.particle('" + particleId + "', '" + locator + "')"), Map.of());
   }
}
