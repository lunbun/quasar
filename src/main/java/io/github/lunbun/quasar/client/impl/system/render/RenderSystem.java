package io.github.lunbun.quasar.client.impl.system.render;

import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.RenderPassManager;
import io.github.lunbun.pulsar.struct.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.struct.pipeline.Shader;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import io.github.lunbun.quasar.client.engine.message.MessageBus;
import io.github.lunbun.quasar.client.engine.message.MessageData;
import io.github.lunbun.quasar.client.engine.message.System;
import io.github.lunbun.quasar.client.impl.message.CreateWindowMessage;
import io.github.lunbun.quasar.client.impl.message.MessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RenderSystem extends System {
    private static final Logger LOGGER = LogManager.getLogger("Quasar");
    private PulsarApplication pulsar;

    @Override
    public void handleMessage(MessageData data) {
        if (data.type == MessageImpl.INIT_SYSTEMS) {
            LOGGER.info("Initializing Render system");
            this.pulsar = new PulsarApplication("Minecraft");
        } else if (data.type == MessageImpl.INIT_WINDOW) {
            LOGGER.info("Initializing window");
            GLFWWindow.disableClientAPI();
            GLFWWindow.setResizable(false);
        } else if (data.type == MessageImpl.CREATE_WINDOW) {
            LOGGER.info("Intercepted window handle");
            this.pulsar.setWindow(((CreateWindowMessage) data).handle);
        } else if (data.type == MessageImpl.INIT_VULKAN) {
            LOGGER.info("Initializing Vulkan");
            GraphicsCardPreference preference = new GraphicsCardPreference(
                    DeviceType.INTEGRATED,
                    new QueueFamily[] { QueueFamily.GRAPHICS, QueueFamily.PRESENT },
                    new DeviceExtension[] { DeviceExtension.SWAP_CHAIN }
            );
            this.pulsar.requestGraphicsCard(preference);
            this.pulsar.initialize();

            Shader shader = new Shader("shader/shader.vert", "shader/shader.frag");
            RenderPass renderPass = this.pulsar.renderPasses.createRenderPass();
            GraphicsPipeline pipeline = this.pulsar.pipelines.createPipeline(shader, renderPass);

            MessageBus.postMessage(MessageImpl.CLEANUP);
        } else if (data.type == MessageImpl.CLEANUP) {
            LOGGER.info("Cleaning up Vulkan");
            this.pulsar.exit();
        }
    }
}
