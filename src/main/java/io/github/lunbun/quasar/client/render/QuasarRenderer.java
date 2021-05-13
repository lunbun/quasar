package io.github.lunbun.quasar.client.render;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.drawing.CommandBatch;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.Shader;
import io.github.lunbun.pulsar.component.vertex.Vertex;
import io.github.lunbun.pulsar.component.vertex.VertexBuffer;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class QuasarRenderer {
    private static final Logger LOGGER = LogManager.getLogger("Quasar");
    private static final PulsarApplication pulsar = new PulsarApplication("Minecraft");
    private static long window;
    private static Vertex.Builder vertexBuilder;
    private static VertexBuffer vbo;

    public static void initWindow() {
        GLFWWindow.disableClientAPI();
        GLFWWindow.setResizable(true);
    }

    public static void createWindow(long handle) {
        window = handle;
        pulsar.setWindow(handle);
    }

    public static void resizeFramebuffer(int width, int height) {
        pulsar.framebufferResized();
    }

    private static void createBuffer() {
        vertexBuilder = new Vertex.Builder();
        vertexBuilder.attribute(Vertex.Type.VEC2, 0);
        vertexBuilder.attribute(Vertex.Type.VEC3, 1);

        Vertex[] vertices = new Vertex[] {
                vertexBuilder.createVertex(new Vector2f(0f, -0.5f), new Vector3f(1, 0, 0)),
                vertexBuilder.createVertex(new Vector2f(0.5f, 0.5f), new Vector3f(0, 1, 0)),
                vertexBuilder.createVertex(new Vector2f(-0.5f, 0.5f), new Vector3f(0, 0, 1))
        };

        vbo = pulsar.vertexBuffers.createVertexBuffer(3, 3L * vertexBuilder.sizeof());
        pulsar.vertexBuffers.uploadData(vbo, vertices);
    }

    private static void createRenderer() {
        Shader shader = new Shader("shader/shader.vert", "shader/shader.frag");
        RenderPass renderPass = pulsar.renderPasses.createRenderPass();
        GraphicsPipeline pipeline = pulsar.pipelines.createPipeline(shader, renderPass, vertexBuilder);
        pulsar.framebuffers.createFramebuffers(renderPass);

        pulsar.commandPool.allocateBuffers(pulsar.framebuffers.framebuffers.size());
        try (CommandBatch batch = pulsar.commandBatches.createBatch()) {
            for (int i = 0; i < pulsar.commandPool.buffers.size(); ++i) {
                CommandBuffer buffer = pulsar.commandPool.buffers.get(i);
                Framebuffer framebuffer = pulsar.framebuffers.framebuffers.get(i);

                buffer.startRecording(batch);
                buffer.startRenderPass(renderPass, framebuffer, batch);
                buffer.bindPipeline(pipeline);
                buffer.bindVertexBuffer(vbo, batch);
                buffer.draw(vbo.count, 1, 0, 0);
                buffer.endRenderPass();
                buffer.endRecording();
            }
        }
    }

    public static void initVulkan() {
        LOGGER.info("Initializing Vulkan");
        GraphicsCardPreference preference = new GraphicsCardPreference(
                DeviceType.INTEGRATED,
                new QueueFamily[] { QueueFamily.GRAPHICS, QueueFamily.PRESENT },
                new DeviceExtension[] { DeviceExtension.SWAP_CHAIN }
        );
        pulsar.requestGraphicsCard(preference);

        pulsar.addRecreateHandler(ignored -> {
            createRenderer();
        });

        pulsar.initialize();

        createBuffer();
        createRenderer();

        System.out.print("0 fps");
        int fps = 0;
        long ms = 0;
        long start = System.currentTimeMillis();

        while (!GLFWWindow.windowShouldClose(window)) {
            GLFWWindow.pollEvents();
            pulsar.frameRenderer.drawFrame(pulsar.commandPool.buffers);

            ++fps;
            long time = System.currentTimeMillis();
            ms += time - start;
            if (ms >= 1000) {
                ms %= 1000;
                System.out.print("\r" + fps + " fps                        ");
                fps = 0;
            }
            start = time;
        }
        System.out.println("\rcomplete                        ");
        pulsar.endLoop();
    }

    public static void cleanup() {
        pulsar.exit();
    }
}
