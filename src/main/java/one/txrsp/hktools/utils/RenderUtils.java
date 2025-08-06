package one.txrsp.hktools.utils;


import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RenderUtils {
    public static final RenderPipeline FILLED_BOX = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation("pipeline/filled_box")
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    private static final RenderLayer renderLayer = RenderLayer.of(
            "filled_box",
            4194304,
            FILLED_BOX,
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );

    public static void renderFilledBox(MatrixStack matrices, BlockPos pos, float r, float g, float b, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        Box box = new Box(pos).offset(-camPos.x, -camPos.y, -camPos.z);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        drawBoxFaces(buffer, box, r, g, b, alpha);

        BuiltBuffer built = buffer.end();

        renderLayer.draw(built);
    }

    private static void drawBoxFaces(VertexConsumer buffer, Box box, float r, float g, float b, float a) {

        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Bottom face
        buffer.vertex(x1, y1, z1).color(r, g, b, a);
        buffer.vertex(x2, y1, z1).color(r, g, b, a);
        buffer.vertex(x2, y1, z2).color(r, g, b, a);
        buffer.vertex(x1, y1, z2).color(r, g, b, a);

        // Top face
        buffer.vertex(x1, y2, z1).color(r, g, b, a);
        buffer.vertex(x1, y2, z2).color(r, g, b, a);
        buffer.vertex(x2, y2, z2).color(r, g, b, a);
        buffer.vertex(x2, y2, z1).color(r, g, b, a);

        // Sides
        buffer.vertex(x1, y1, z1).color(r, g, b, a);
        buffer.vertex(x1, y2, z1).color(r, g, b, a);
        buffer.vertex(x2, y2, z1).color(r, g, b, a);
        buffer.vertex(x2, y1, z1).color(r, g, b, a);

        buffer.vertex(x1, y1, z2).color(r, g, b, a);
        buffer.vertex(x2, y1, z2).color(r, g, b, a);
        buffer.vertex(x2, y2, z2).color(r, g, b, a);
        buffer.vertex(x1, y2, z2).color(r, g, b, a);

        buffer.vertex(x1, y1, z1).color(r, g, b, a);
        buffer.vertex(x1, y1, z2).color(r, g, b, a);
        buffer.vertex(x1, y2, z2).color(r, g, b, a);
        buffer.vertex(x1, y2, z1).color(r, g, b, a);

        buffer.vertex(x2, y1, z1).color(r, g, b, a);
        buffer.vertex(x2, y2, z1).color(r, g, b, a);
        buffer.vertex(x2, y2, z2).color(r, g, b, a);
        buffer.vertex(x2, y1, z2).color(r, g, b, a);
    }
}
