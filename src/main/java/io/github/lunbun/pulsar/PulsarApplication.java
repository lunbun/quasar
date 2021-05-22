package io.github.lunbun.pulsar;

import io.github.lunbun.pulsar.component.drawing.BlockingTimer;
import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.drawing.FrameSynchronizer;
import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.Shader;
import io.github.lunbun.pulsar.component.presentation.ImageViewsManager;
import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.SwapChainManager;
import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.component.setup.*;
import io.github.lunbun.pulsar.component.texture.Texture;
import io.github.lunbun.pulsar.component.texture.TextureSampler;
import io.github.lunbun.pulsar.component.uniform.DescriptorPool;
import io.github.lunbun.pulsar.component.uniform.DescriptorSetLayout;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.component.vertex.MemoryAllocator;
import io.github.lunbun.pulsar.struct.setup.DeviceExtension;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.util.misc.CommandBufferRecorder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A class that provides easy interaction with Vulkan.
 */
public final class PulsarApplication {
    public static final Logger LOGGER = LogManager.getLogger("Pulsar");
    public static final int MAX_FRAMES_IN_FLIGHT = FrameSynchronizer.MAX_FRAMES_IN_FLIGHT;

    public final String name;

    private Instance instance;
    private LogicalDevice logicalDevice;
    private final QueueManager queues;
    private WindowSurface surface;
    private SwapChain swapChain;
    private final SwapChainManager swapChainManager;
    private MemoryAllocator memoryAllocator;
    private final ImageViewsManager imageViews;
    private CommandPool commandPool;

    private GraphicsCardPreference graphicsCardPreference;
    private long windowHandle;
    private final List<CommandBufferRecorder> commandBufferRecorders;

    public RenderPass.Builder renderPasses;
    public GraphicsPipeline.Builder pipelines;
    public Shader.Builder shaders;
    public Framebuffer.Builder framebuffers;
    public BlockingTimer.Builder timings;
    public FrameSynchronizer frameRenderer;
    public Buffer.Builder buffers;
    public DescriptorSetLayout.Builder descriptorSetLayouts;
    public DescriptorPool descriptorPool;
    public Texture.Loader textureLoader;
    public TextureSampler.Builder textureSamplers;

    public PulsarApplication(String name) {
        this.name = name;

        this.queues = new QueueManager();
        this.imageViews = new ImageViewsManager();
        this.swapChainManager = new SwapChainManager();
        this.commandBufferRecorders = new ObjectArrayList<>();
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

    public int getSwapWidth() {
        return this.swapChain.extent.width();
    }

    public int getSwapHeight() {
        return this.swapChain.extent.height();
    }

    public void initialize() {
        this.instance = Instance.Builder.createInstance(this.name);
        LOGGER.info("Created Vulkan instance");

        ValidationLayerUtils.setupDebugMessenger(this.instance);
        LOGGER.info("Setup debug messenger");

        this.surface = WindowSurface.Builder.createSurface(this.instance, this.windowHandle);
        LOGGER.info("Created window surface");

        PhysicalDevice physicalDevice = PhysicalDevice.Selector.choosePhysicalDevice(this.instance, this.surface, this.graphicsCardPreference);
        LOGGER.info("Chose physical device");
        LOGGER.info("Using " + physicalDevice.vendor + " GPU " + physicalDevice.name);

        this.logicalDevice = LogicalDevice.Builder.createLogicalDevice(physicalDevice, this.surface, this.graphicsCardPreference, this.queues);
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
        this.swapChain = SwapChain.Builder.createSwapChain(physicalDevice, this.logicalDevice, this.surface,
                this.windowHandle, this.graphicsCardPreference);
        LOGGER.info("Created swap chain");

        this.imageViews.createImageViews(this.logicalDevice, swapChain);
        LOGGER.info("Created image views");

        this.memoryAllocator = new MemoryAllocator(this.logicalDevice);
        this.shaders = new Shader.Builder(this.logicalDevice);
        this.pipelines = new GraphicsPipeline.Builder(this.logicalDevice, this.shaders, swapChain);
        this.renderPasses = new RenderPass.Builder(this.logicalDevice, swapChain);
        this.framebuffers = new Framebuffer.Builder(this.logicalDevice, swapChain, this.imageViews);
        this.commandPool = new CommandPool(this.logicalDevice, swapChain, physicalDevice, this.surface, this.graphicsCardPreference);
        this.timings = new BlockingTimer.Builder(this.logicalDevice);
        this.frameRenderer = new FrameSynchronizer(this.logicalDevice, swapChain, this.swapChainManager, this.queues, this.commandPool,
                this.timings);
        this.frameRenderer.init();
        this.frameRenderer.bufferRecorder = (commandBuffer, index, currentFrame) -> {
            commandBuffer.startRecordingOneTimeSubmit();
            for (CommandBufferRecorder recorder : this.commandBufferRecorders) {
                recorder.record(commandBuffer, index, currentFrame);
            }
            commandBuffer.endRecording();
        };
        this.buffers = new Buffer.Builder(this.logicalDevice, physicalDevice, this.commandPool, this.queues, this.memoryAllocator);
        this.descriptorSetLayouts = new DescriptorSetLayout.Builder(this.logicalDevice);
        this.descriptorPool = new DescriptorPool(this.logicalDevice, 2 * MAX_FRAMES_IN_FLIGHT);
        this.textureLoader = new Texture.Loader(this.logicalDevice, physicalDevice, this.memoryAllocator, this.commandPool, this.queues);
        this.textureSamplers = new TextureSampler.Builder(this.logicalDevice, physicalDevice);
        LOGGER.info("Setup pulsar-quasar interaction");

        this.swapChainManager.assign(this.logicalDevice, swapChain, this.framebuffers, this.commandPool,
                this.pipelines, this.renderPasses, this.imageViews, this.windowHandle);
        this.addCommandBufferDestructor(ignored -> {
            this.frameRenderer.freeBuffers();
        });
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

    public void addCommandBufferRecorder(CommandBufferRecorder handler) {
        this.commandBufferRecorders.add(handler);
    }

    public void exit() {
        this.swapChainManager.cleanup();
        this.memoryAllocator.destroy();
        this.descriptorPool.destroy();
        this.timings.destroy();
        this.commandPool.destroy();
        this.logicalDevice.destroy();
        ValidationLayerUtils.destroy(this.instance);
        this.surface.destroy();
        this.instance.destroy();
    }
}
