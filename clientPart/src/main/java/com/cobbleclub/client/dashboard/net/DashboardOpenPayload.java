package com.cobbleclub.client.dashboard.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record DashboardOpenPayload(String json) implements class_8710 {
   public static final class_8710.class_9154<DashboardOpenPayload> TYPE = new class_8710.class_9154<>(class_2960.method_60655("cobbleclub", "dashboard_open/v1"));
   public static final class_9139<class_2540, DashboardOpenPayload> STREAM_CODEC = class_9139.method_56437(
           (buf, payload) -> buf.method_10788(payload.json, 32768),
           buf -> new DashboardOpenPayload(buf.method_10800(32768))
   );

   public class_8710.class_9154<? extends class_8710> method_56479() { return TYPE; }
}
