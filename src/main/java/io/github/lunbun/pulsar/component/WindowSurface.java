package io.github.lunbun.pulsar.component;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK10;

import java.nio.LongBuffer;

public final class WindowSurface {
    public long surface;

    public void createSurface(InstanceManager instance, long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.longs(VK10.VK_NULL_HANDLE);

            if (GLFWVulkan.glfwCreateWindowSurface(instance.instance, window, null, pSurface) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }

            this.surface = pSurface.get(0);
        }
    }

    public void destroy(InstanceManager instance) {
        KHRSurface.vkDestroySurfaceKHR(instance.instance, this.surface, null);
    }
}
