package io.github.lunbun.pulsar.component.presentation;

import io.github.lunbun.pulsar.component.setup.Instance;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK10;

import java.nio.LongBuffer;

public final class WindowSurface {
    public final long surface;
    private final Instance instance;

    protected WindowSurface(long surface, Instance instance) {
        this.surface = surface;
        this.instance = instance;
    }

    public void destroy() {
        KHRSurface.vkDestroySurfaceKHR(this.instance.instance, this.surface, null);
    }

    public static final class Builder {
        public static WindowSurface createSurface(Instance instance, long window) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer pSurface = stack.longs(VK10.VK_NULL_HANDLE);

                if (GLFWVulkan.glfwCreateWindowSurface(instance.instance, window, null, pSurface) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create window surface");
                }

                return new WindowSurface(pSurface.get(0), instance);
            }
        }
    }
}
