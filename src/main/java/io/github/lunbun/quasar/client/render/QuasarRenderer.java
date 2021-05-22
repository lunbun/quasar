package io.github.lunbun.quasar.client.render;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.quasar.Quasar;
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import io.github.lunbun.quasar.client.render.immediate.Immediates;
import io.github.lunbun.quasar.client.render.test.TestRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class QuasarRenderer {
    private static final PulsarApplication pulsar = new PulsarApplication("Minecraft");
    private static final List<VulkanRenderer> vulkanRenderers = new ObjectArrayList<>();
    private static long window;

    public static void initWindow() {
        GLFWWindow.disableClientAPI();
        GLFWWindow.setResizable(true);
    }

    public static void createWindow(long handle) {
        window = handle;
        pulsar.setWindow(handle);
    }

    private static void createVulkanRenderers() {
        vulkanRenderers.add(new TestRenderer());
//        vulkanRenderers.add(new Immediates());
    }

    public static void resizeFramebuffer(int width, int height) {
        pulsar.framebufferResized();
    }

    public static void initVulkan() {
        Quasar.LOGGER.info("Initializing Vulkan");
        createVulkanRenderers();

        GraphicsCardPreference preference = new GraphicsCardPreference(
                DeviceType.INTEGRATED,
                new QueueFamily[] { QueueFamily.GRAPHICS, QueueFamily.PRESENT },
                new DeviceExtension[] { DeviceExtension.SWAP_CHAIN }
        );
        pulsar.requestGraphicsCard(preference);

        pulsar.addRecreateHandler(ignored -> {
            for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
                vulkanRenderer.recreateFramebuffers();
            }
        });

        pulsar.addCommandBufferDestructor(ignored -> {
            for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
                vulkanRenderer.destroyFramebuffers();
            }
        });

        pulsar.addCommandBufferRecorder((commandBuffer, index, currentFrame) -> {
            for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
                vulkanRenderer.recordCommandBuffers(commandBuffer, index, currentFrame);
            }
        });

        pulsar.initialize();

        for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
            vulkanRenderer.init(pulsar);
        }
        for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
            vulkanRenderer.recreateFramebuffers();
        }

        System.out.print("0 fps");
        int fps = 0;
        long ms = 0;
        long start = System.currentTimeMillis();

        while (!GLFWWindow.windowShouldClose(window)) {
            GLFWWindow.pollEvents();
//            DrawableHelper.fill(new MatrixStack(), 0, 0, 1, 1, 0xffffffff);
            pulsar.frameRenderer.drawFrame();

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

        cleanup();
    }

    public static void cleanup() {
        for (VulkanRenderer vulkanRenderer : vulkanRenderers) {
            vulkanRenderer.destroy();
        }
        pulsar.exit();
    }
}
