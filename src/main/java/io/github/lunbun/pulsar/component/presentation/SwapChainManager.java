package io.github.lunbun.pulsar.component.presentation;

import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.function.Consumer;

public final class SwapChainManager {
    public SwapChain swapChain;
    public LogicalDevice device;
    public final List<Consumer<Void>> swapChainHandlers;

    private long window;
    private Framebuffer.Builder framebuffers;
    private CommandPool commandPool;
    private GraphicsPipeline.Builder pipelines;
    private RenderPass.Builder renderPasses;
    private ImageViewsManager imageViews;

    public SwapChainManager() {
        this.swapChainHandlers = new ObjectArrayList<>();
    }

    public void assign(LogicalDevice device, SwapChain swapChain, Framebuffer.Builder framebuffers,
                       CommandPool commandPool, GraphicsPipeline.Builder pipelines, RenderPass.Builder renderPasses,
                       ImageViewsManager imageViews, long window) {
        this.device = device;
        this.swapChain = swapChain;
        this.framebuffers = framebuffers;
        this.commandPool = commandPool;
        this.pipelines = pipelines;
        this.renderPasses = renderPasses;
        this.imageViews = imageViews;
        this.window = window;
    }

    public void cleanup() {
        this.framebuffers.destroy();
        this.commandPool.freeBuffers();
        this.pipelines.destroy();
        this.renderPasses.destroy();
        this.imageViews.destroy();
        this.swapChain.destroy();
    }

    public void recreate() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(this.window, width, height);
            while (width.get(0) == 0 || height.get(0) == 0) {
                GLFW.glfwGetFramebufferSize(this.window, width, height);
                GLFW.glfwWaitEvents();
            }

            this.device.waitIdle();
            this.cleanup();
            this.swapChain.create();
            this.imageViews.createImageViews(this.device, this.swapChain);
            this.executeHandlers();
        }
    }

    public void executeHandlers() {
        for (Consumer<Void> handlers : this.swapChainHandlers) {
            handlers.accept(null);
        }
    }
}
