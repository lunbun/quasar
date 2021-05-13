package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Map;

public final class VertexBuffer {
    public final long buffer;
    public final int memoryType;
    public final long memory;
    public final int pointer;
    public final long size;
    public final int allocSize;
    public final int count;

    private final Builder builder;
    private final long index;

    protected VertexBuffer(Builder builder, long index, long buffer, int memoryType, long memory, int pointer, long size,
                           int allocSize, int count) {
        this.builder = builder;
        this.index = index;

        this.buffer = buffer;
        this.memoryType = memoryType;
        this.memory = memory;
        this.pointer = pointer;
        this.size = size;
        this.allocSize = allocSize;
        this.count = count;
    }

    public void destroy() {
        this.builder.destroy(this.index);
    }

    public static final class Builder {
        private final LogicalDevice device;
        private final PhysicalDevice physicalDevice;
        private final MemoryAllocator allocator;
        private final Map<Long, VertexBuffer> buffers;
        private long index;

        public Builder(LogicalDevice device, PhysicalDevice physicalDevice, MemoryAllocator allocator) {
            this.device = device;
            this.physicalDevice = physicalDevice;
            this.allocator = allocator;
            this.buffers = new Long2ObjectOpenHashMap<>();
            this.index = 0;
        }

        protected void destroy(long index) {
            VertexBuffer buffer = this.buffers.get(index);
            VK10.vkDestroyBuffer(this.device.device, buffer.buffer, null);
            this.allocator.free(buffer.memoryType, buffer.pointer, buffer.allocSize);
            this.buffers.remove(index);
        }

        public void destroy() {
            for (Map.Entry<Long, VertexBuffer> buffer : this.buffers.entrySet()) {
                this.destroy(buffer.getKey());
            }
        }

        private int findMemoryType(int typeFilter, int properties) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.callocStack(stack);
                VK10.vkGetPhysicalDeviceMemoryProperties(this.physicalDevice.device, memProperties);

                for (int i = 0; i < memProperties.memoryTypeCount(); ++i) {
                    if (((typeFilter & (1 << i)) != 0) &&
                            ((memProperties.memoryTypes(i).propertyFlags() & properties) == properties)) {
                        return i;
                    }
                }

                throw new RuntimeException("Failed to find suitable memory type!");
            }
        }

        public VertexBuffer createVertexBuffer(int count, long size) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
                bufferInfo.sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
                bufferInfo.size(size);
                bufferInfo.usage(VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
                bufferInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);

                LongBuffer pVertexBuffer = stack.mallocLong(1);

                if (VK10.vkCreateBuffer(this.device.device, bufferInfo, null, pVertexBuffer) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create vertex buffer!");
                }

                long vertexBuffer = pVertexBuffer.get(0);

                VkMemoryRequirements memRequirements = VkMemoryRequirements.callocStack(stack);
                VK10.vkGetBufferMemoryRequirements(this.device.device, vertexBuffer, memRequirements);

                int memoryType = this.findMemoryType(memRequirements.memoryTypeBits(),
                        VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

                long vertexBufferMemory = this.allocator.heap(memoryType);
                int vertexBufferPointer = this.allocator.malloc(memoryType, (int) memRequirements.size());

                VK10.vkBindBufferMemory(this.device.device, vertexBuffer, vertexBufferMemory, vertexBufferPointer);

                long index = ++this.index;
                if (this.index == -1) {
                    throw new RuntimeException("Ran out of buffer ids!");
                }

                VertexBuffer vbo = new VertexBuffer(this, index, vertexBuffer, memoryType, vertexBufferMemory,
                        vertexBufferPointer, bufferInfo.size(), (int) memRequirements.size(), count);
                this.buffers.put(index, vbo);
                return vbo;
            }
        }

        public void uploadData(VertexBuffer buffer, Vertex[] vertices) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pData = stack.mallocPointer(1);
                VK10.vkMapMemory(this.device.device, buffer.memory, buffer.pointer, buffer.size, 0, pData);
                {
                    ByteBuffer byteBuffer = pData.getByteBuffer(0, (int) buffer.size);
                    for (Vertex vertex : vertices) {
                        vertex.write(byteBuffer);
                    }
                }
                VK10.vkUnmapMemory(this.device.device, buffer.memory);
            }
        }
    }
}
