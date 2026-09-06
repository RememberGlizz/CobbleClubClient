package com.cobbleclub.client.dashboard.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

@Environment(EnvType.CLIENT)
public record DashboardActionPayload(String action) implements class_8710 {
   public static final class_8710.class_9154<DashboardActionPayload> TYPE = new class_8710.class_9154<>(class_2960.method_60655("cobbleclub", "dashboard_action/v1"));
   public static final class_9139<class_2540, DashboardActionPayload> STREAM_CODEC = class_9139.method_56437(
           (buf, payload) -> buf.method_10788(payload.action, 64),
           buf -> new DashboardActionPayload(buf.method_10800(64))
   );

   public class_8710.class_9154<? extends class_8710> method_56479() { return TYPE; }
}
