package io.github.lunbun.pulsar;

import io.github.lunbun.pulsar.component.drawing.*;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.Shader;
import io.github.lunbun.pulsar.component.presentation.ImageViewsManager;
import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.SwapChainManager;
import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.component.setup.Instance;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.component.vertex.IndexBuffer;
import io.github.lunbun.pulsar.component.vertex.MemoryAllocator;
import io.github.lunbun.pulsar.component.vertex.VertexBuffer;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.component.setup.ValidationLayerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A class that provides easy interaction with Vulkan.
 */
public final class PulsarApplication {
    private static final Logger LOGGER = LogManager.getLogger("Pulsar");

    public final String name;

    private Instance instance;
    private PhysicalDevice physicalDevice;
    private LogicalDevice logicalDevice;
    private final QueueManager queues;
    private WindowSurface surface;
    private SwapChain swapChain;
    private SwapChainManager swapChainManager;
    private MemoryAllocator memoryAllocator;
    private final ImageViewsManager imageViews;

    private GraphicsCardPreference graphicsCardPreference;
    private long windowHandle;

    public RenderPass.Builder renderPasses;
    public GraphicsPipeline.Builder pipelines;
    public Shader.Builder shaders;
    public Framebuffer.Builder framebuffers;
    public CommandPool commandPool;
    public CommandBatch.Builder commandBatches;
    public BlockingTimer.Builder timings;
    public FrameSynchronizer frameRenderer;
    public VertexBuffer.Builder vertexBuffers;
    public IndexBuffer.Builder indexBuffers;

    public PulsarApplication(String name) {
        this.name = name;

        this.queues = new QueueManager();
        this.imageViews = new ImageViewsManager();
        this.swapChainManager = new SwapChainManager();
    }

    public void requestGraphicsCard(GraphicsCardPreference preference) {
        this.graphicsCardPreference = preference;
    }

    public void setWindow(long window) {
        this.windowHandle = window;
    }

    public void framebufferResized() {
        this.frameRenderer.framebufferResized = true;
    }

    public void initialize() {
        this.instance = Instance.Builder.createInstance(this.name);
        LOGGER.info("Created Vulkan instance");

        ValidationLayerUtils.setupDebugMessenger(this.instance);
        LOGGER.info("Setup debug messenger");

        this.surface = WindowSurface.Builder.createSurface(this.instance, this.windowHandle);
        LOGGER.info("Created window surface");

        this.physicalDevice = PhysicalDevice.Selector.choosePhysicalDevice(this.instance, this.surface, this.graphicsCardPreference);
        LOGGER.info("Chose physical device");
        LOGGER.info("Using " + this.physicalDevice.vendor + " GPU " + this.physicalDevice.name);

        this.logicalDevice = LogicalDevice.Builder.createLogicalDevice(this.physicalDevice, this.surface, this.graphicsCardPreference, this.queues);
        LOGGER.info("Created logical device");
        LOGGER.info("Using " + this.queues.getQueueFamilies().stream()
                .map(QueueFamily::toString)
                .map(String::toLowerCase)
                .collect(Collectors.joining(" queue, ")) + " queue");
        LOGGER.info("Using " + this.graphicsCardPreference.extensions.stream()
                .map(DeviceExtension::toString)
                .map(String::toLowerCase)
                .collect(Collectors.joining(" extension, ")) + " extension");

        if (!this.graphicsCardPreference.hasSwapChain) {
            throw new RuntimeException("Swap chain required!");
        }
        this.swapChain = SwapChain.Builder.createSwapChain(this.physicalDevice, this.logicalDevice, this.surface,
                this.windowHandle, this.graphicsCardPreference);
        LOGGER.info("Created swap chain");

        this.imageViews.createImageViews(this.logicalDevice, this.swapChain);
        LOGGER.info("Created image views");

        this.memoryAllocator = new MemoryAllocator(this.logicalDevice);
        this.shaders = new Shader.Builder(this.logicalDevice);
        this.pipelines = new GraphicsPipeline.Builder(this.logicalDevice, this.shaders, this.swapChain);
        this.renderPasses = new RenderPass.Builder(this.logicalDevice, this.swapChain);
        this.framebuffers = new Framebuffer.Builder(this.logicalDevice, this.swapChain, this.imageViews);
        this.commandPool = new CommandPool(this.logicalDevice, this.swapChain, this.physicalDevice, this.surface, this.graphicsCardPreference);
        this.commandBatches = new CommandBatch.Builder(this.swapChain);
        this.timings = new BlockingTimer.Builder(this.logicalDevice);
        this.frameRenderer = new FrameSynchronizer(this.logicalDevice, this.swapChain, this.swapChainManager, this.queues, this.timings);
        this.frameRenderer.init();
        this.vertexBuffers = new VertexBuffer.Builder(this.logicalDevice, this.physicalDevice, this.commandPool, this.commandBatches, this.queues, this.memoryAllocator);
        this.indexBuffers = new IndexBuffer.Builder(this.logicalDevice, this.physicalDevice, this.commandPool, this.commandBatches, this.queues, this.memoryAllocator);
        LOGGER.info("Setup pulsar-quasar interaction");

        this.swapChainManager.assign(this.logicalDevice, this.swapChain, this.framebuffers, this.commandPool,
                this.pipelines, this.renderPasses, this.imageViews, this.windowHandle);
    }

    public void endLoop() {
        this.frameRenderer.endLoop();
    }

    public void addRecreateHandler(Consumer<Void> handler) {
        this.swapChainManager.swapChainHandlers.add(handler);
    }

    public void addCommandBufferDestructor(Consumer<Void> handler) {
        this.swapChainManager.commandBufferDestructors.add(handler);
    }

    public void exit() {
        this.swapChainManager.cleanup();
        this.memoryAllocator.destroy();
        this.timings.destroy();
        this.commandPool.destroy();
        this.logicalDevice.destroy();
        ValidationLayerUtils.destroy(this.instance);
        this.surface.destroy();
        this.instance.destroy();
    }
}
