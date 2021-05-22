package io.github.lunbun.quasar.client.render;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;

/**
 * A renderer which uses Pulsar to interact with Vulkan.
 */
public interface VulkanRenderer {
    /**
     * Initialize all Pulsar elements the renderer will use.
     * @param pulsar the Pulsar application
     */
    void init(PulsarApplication pulsar);

    /**
     * Destroy all Pulsar elements that have been created.
     */
    void destroy();

    /**
     * Called on startup and when the window is resized.
     * Recreate render passes, graphics pipelines, and framebuffers here.
     */
    void recreateFramebuffers();

    /**
     * Called during swap chain recreation when framebuffers are destroyed.
     * Destroy only the framebuffers.
     */
    void destroyFramebuffers();

    /**
     * Record rendering data into a command buffer. This is called every frame, and you can use this to run events each
     * frame.
     * @param buffer the command buffer to record into
     * @param framebufferIndex the index of the swap chain image to get the framebuffer from
     * @param currentFrame the index of the current frame in flight
     */
    void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame);
}
