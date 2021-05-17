package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.util.vulkan.DeviceUtils;
import io.github.lunbun.pulsar.util.vulkan.QueueFamilyIndices;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

public final class CommandPool {
    private final LogicalDevice device;
    private final SwapChain swapChain;

    private final long commandPool;

    public CommandPool(LogicalDevice device, SwapChain swapChain, PhysicalDevice physicalDevice, WindowSurface surface, GraphicsCardPreference preference) {
        this.swapChain = swapChain;
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(physicalDevice.device, surface.surface, preference);

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(indices.getFamilyIndex(QueueFamily.GRAPHICS));

            LongBuffer pCommandPool = stack.longs(VK10.VK_NULL_HANDLE);

            if (VK10.vkCreateCommandPool(device.device, poolInfo, null, pCommandPool) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool!");
            }

            this.commandPool = pCommandPool.get(0);
        }
    }

    public CommandBuffer allocateBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(this.commandPool);
            allocInfo.level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if (VK10.vkAllocateCommandBuffers(this.device.device, allocInfo, pCommandBuffers) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers!");
            }

            return new CommandBuffer(this.swapChain, new VkCommandBuffer(pCommandBuffers.get(0), this.device.device));
        }
    }

    public void allocateBuffers(int count, List<CommandBuffer> buffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(this.commandPool);
            allocInfo.level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(count);

            PointerBuffer pCommandBuffers = stack.mallocPointer(count);

            if (VK10.vkAllocateCommandBuffers(this.device.device, allocInfo, pCommandBuffers) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers!");
            }

            for (int i = 0; i < count; ++i) {
                buffers.add(new CommandBuffer(this.swapChain, new VkCommandBuffer(pCommandBuffers.get(i), this.device.device)));
            }
        }
    }

    public void destroy() {
        VK10.vkDestroyCommandPool(this.device.device, this.commandPool, null);
    }

    public void freeBuffers(List<CommandBuffer> buffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffers = stack.mallocPointer(buffers.size());
            for (int i = buffers.size() - 1; i >= 0; --i) {
                pBuffers.put(i, buffers.get(i).buffer);
                buffers.remove(i);
            }
            VK10.vkFreeCommandBuffers(this.device.device, this.commandPool, pBuffers);
        }
    }

    public void freeBuffer(CommandBuffer buffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffers = stack.mallocPointer(1);
            pBuffers.put(0, buffer.buffer);
            VK10.vkFreeCommandBuffers(this.device.device, this.commandPool, pBuffers);
        }
    }
}
