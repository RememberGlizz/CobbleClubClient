package com.cobbleclub.client.chestshop;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

@Environment(EnvType.CLIENT)
public final class ChestShopPlacementRenderer {
    private static volatile boolean active;
    private static volatile String message;
    private static final RenderLayer PREVIEW;

    private ChestShopPlacementRenderer() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register(ChestShopPlacementRenderer::renderHud);
        WorldRenderEvents.AFTER_ENTITIES.register(ChestShopPlacementRenderer::renderWorld);
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ChestShopPlacementRenderer.setActive(false, "")
        );
    }

    public static void setActive(boolean value, String text) {
        active = value;

        if (text != null && !text.isBlank()) {
            message = text;
        }
    }

    private static void renderHud(DrawContext g, RenderTickCounter tickDelta) {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();

        String title = "✦ Chest Shop Placement ✦";
        String line = message == null || message.isBlank()
                ? "Right click the side of a chest to place the sign"
                : message;

        int boxW = Math.max(
                client.textRenderer.getWidth(title),
                client.textRenderer.getWidth(line)
        ) + 18;

        int x = (w - boxW) / 2;
        int y = client.getWindow().getScaledHeight() - 66;

        g.fill(
                x,
                y,
                x + boxW,
                y + 39,
                -1206644704
        );

        g.drawBorder(
                x,
                y,
                boxW,
                39,
                -6593537
        );

        g.drawCenteredTextWithShadow(
                client.textRenderer,
                title,
                w / 2,
                y + 7,
                -1583617
        );

        g.drawCenteredTextWithShadow(
                client.textRenderer,
                line,
                w / 2,
                y + 22,
                -2631705
        );
    }

    private static void renderWorld(WorldRenderContext context) {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        HitResult hitResult = client.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult hit)) {
            return;
        }

        if (context.matrixStack() == null || context.consumers() == null) {
            return;
        }

        BlockPos chestPos = hit.getBlockPos();
        BlockState chestState = client.world.getBlockState(chestPos);

        if (!(chestState.getBlock() instanceof ChestBlock)) {
            return;
        }

        Direction side = hit.getSide();
        if (side == Direction.UP || side == Direction.DOWN) {
            return;
        }

        BlockPos signPos = chestPos.offset(side);

        BlockState signState = Blocks.DARK_OAK_WALL_SIGN
                .getDefaultState()
                .with(WallSignBlock.FACING, side);

        boolean valid =
                client.world.getBlockState(signPos).isAir()
                        && signState.canPlaceAt(client.world, signPos);

        float r = valid ? 0.55f : 1.0f;
        float g = valid ? 0.25f : 0.2f;
        float b = valid ? 1.0f : 0.2f;

        double inset = 0.17;
        double thickness = 0.065;

        Box box;

        if (side == Direction.NORTH || side == Direction.SOUTH) {
            double z0 = side == Direction.NORTH
                    ? signPos.getZ() + 1.0 - thickness
                    : signPos.getZ();

            box = new Box(
                    signPos.getX() + inset,
                    signPos.getY() + 0.25,
                    z0,
                    signPos.getX() + 1.0 - inset,
                    signPos.getY() + 0.82,
                    z0 + thickness
            );
        } else {
            double x0 = side == Direction.WEST
                    ? signPos.getX() + 1.0 - thickness
                    : signPos.getX();

            box = new Box(
                    x0,
                    signPos.getY() + 0.25,
                    signPos.getZ() + inset,
                    x0 + thickness,
                    signPos.getY() + 0.82,
                    signPos.getZ() + 1.0 - inset
            );
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumer quads = context.consumers().getBuffer(PREVIEW);
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());

        WorldRenderer.renderFilledBox(
                matrices,
                quads,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ,
                r,
                g,
                b,
                0.22f
        );

        WorldRenderer.drawBox(
                matrices,
                lines,
                box,
                r,
                g,
                b,
                0.95f
        );
    }

    static {
        message = "Right click a chest to place the shop sign";

        PREVIEW = RenderLayer.of(
                "cobbleclub_chestshop_preview",
                VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.TRIANGLE_STRIP,
                512,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.COLOR_PROGRAM)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .build(false)
        );
    }
}
