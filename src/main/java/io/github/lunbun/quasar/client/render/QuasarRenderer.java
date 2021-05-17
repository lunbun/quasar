package io.github.lunbun.quasar.client.render;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import io.github.lunbun.quasar.client.render.stage.Immediate2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuasarRenderer {
    private static final Logger LOGGER = LogManager.getLogger("Quasar");
    public static final PulsarApplication pulsar = new PulsarApplication("Minecraft");
    private static long window;

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

    public static void initVulkan() {
        LOGGER.info("Initializing Vulkan");
        GraphicsCardPreference preference = new GraphicsCardPreference(
                DeviceType.INTEGRATED,
                new QueueFamily[] { QueueFamily.GRAPHICS, QueueFamily.PRESENT },
                new DeviceExtension[] { DeviceExtension.SWAP_CHAIN }
        );
        pulsar.requestGraphicsCard(preference);

        pulsar.addRecreateHandler(ignored -> {
            Immediate2D.recreateFramebuffers();
        });

        pulsar.addCommandBufferDestructor(ignored -> {
            Immediate2D.destroyFramebuffers();
        });

        pulsar.addCommandBufferRecorder((commandBuffer, index, currentFrame) -> {
            Immediate2D.recordCommandBuffers(commandBuffer, index, currentFrame);
        });

        pulsar.initialize();

        Immediate2D.initPulsar();
        Immediate2D.recreateFramebuffers();

        System.out.print("0 fps");
        int fps = 0;
        long ms = 0;
        long start = System.currentTimeMillis();

        while (!GLFWWindow.windowShouldClose(window)) {
            GLFWWindow.pollEvents();
            Immediate2D.clear();
            Immediate2D.fill(-0.5f, -0.5f, 0.5f, 0.5f, 1, 1, 1, 1);
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
        Immediate2D.destroy();
        pulsar.exit();
    }
}
