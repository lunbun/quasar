package io.github.lunbun.pulsar;

import io.github.lunbun.pulsar.component.*;
import io.github.lunbun.pulsar.struct.DeviceExtension;
import io.github.lunbun.pulsar.struct.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.QueueFamily;
import io.github.lunbun.pulsar.util.ValidationLayerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that provides easy interaction with Vulkan.
 */
public final class PulsarApplication {
    private static final Logger LOGGER = LogManager.getLogger("Pulsar");

    public final String name;

    private final InstanceManager instance;
    private final PhysicalDeviceManager physicalDevice;
    private final LogicalDeviceManager logicalDevice;
    private final QueueManager queues;
    private final WindowSurface windowSurface;
    private final SwapChain swapChain;

    private GraphicsCardPreference graphicsCardPreference;
    private long windowHandle;

    public PulsarApplication(String name) {
        this.name = name;

        this.instance = new InstanceManager();
        this.physicalDevice = new PhysicalDeviceManager();
        this.logicalDevice = new LogicalDeviceManager();
        this.queues = new QueueManager();
        this.windowSurface = new WindowSurface();
        this.swapChain = new SwapChain();
    }

    public void requestGraphicsCard(GraphicsCardPreference preference) {
        this.graphicsCardPreference = preference;
    }

    public void setWindow(long window) {
        this.windowHandle = window;
    }

    public void initialize() {
        this.instance.createInstance(this.name);
        LOGGER.info("Created Vulkan instance");

        ValidationLayerUtils.setupDebugMessenger(this.instance);
        LOGGER.info("Setup debug messenger");

        this.windowSurface.createSurface(this.instance, this.windowHandle);
        LOGGER.info("Created window surface");

        this.physicalDevice.preference = this.graphicsCardPreference;
        this.physicalDevice.surface = this.windowSurface;
        this.physicalDevice.pickPhysicalDevice(this.instance);
        LOGGER.info("Chose physical device");
        LOGGER.info("Using " + this.physicalDevice.vendor + " GPU " + this.physicalDevice.name);

        this.logicalDevice.windowSurface = this.windowSurface;
        this.logicalDevice.preference = this.graphicsCardPreference;
        this.logicalDevice.createLogicalDevice(this.physicalDevice, this.queues);
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
        this.swapChain.logicalDevice = this.logicalDevice;
        this.swapChain.physicalDevice = this.physicalDevice;
        this.swapChain.surface = this.windowSurface;
        this.swapChain.preference = this.graphicsCardPreference;
        this.swapChain.window = this.windowHandle;
        this.swapChain.createSwapChain();
        LOGGER.info("Created swap chain");
    }

    public void exit() {
        this.swapChain.destroy();

        this.logicalDevice.destroy();

        ValidationLayerUtils.destroy(this.instance);

        this.windowSurface.destroy(this.instance);

        this.instance.destroyInstance();
    }
}
