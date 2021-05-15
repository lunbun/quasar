package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.component.drawing.CommandBatch;
import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.struct.vertex.BufferData;
import io.github.lunbun.pulsar.util.vulkan.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class Buffer extends BufferData {
    public final int count;

    private final Builder builder;

    protected Buffer(Builder builder, int count, BufferData bufferData) {
        super(bufferData);
        this.builder = builder;
        this.count = count;
    }

    public void destroy() {
        this.builder.destroy(this);
    }

    public enum Type {
        VERTEX(VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
        INDEX(VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT);

        public final int usage;

        Type(int usage) {
            this.usage = usage;
        }
    }

    public static final class Builder {
        private final LogicalDevice device;
        private final PhysicalDevice physicalDevice;
        private final CommandPool commandPool;
        private final CommandBatch.Builder commandBatches;
        private final QueueManager queues;
        private final MemoryAllocator allocator;

        public Builder(LogicalDevice device, PhysicalDevice physicalDevice, CommandPool commandPool, CommandBatch.Builder commandBatches, QueueManager queues, MemoryAllocator allocator) {
            this.device = device;
            this.physicalDevice = physicalDevice;
            this.commandPool = commandPool;
            this.commandBatches = commandBatches;
            this.queues = queues;
            this.allocator = allocator;
        }

        protected void destroy(Buffer buffer) {
            BufferUtils.destroy(this.device, this.allocator, buffer);
        }

        public Buffer createBuffer(Type type, int count, long size) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                BufferData bufferData = BufferUtils.createBuffer(this.device, this.physicalDevice, this.allocator, size,
                        VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | type.usage, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack);

                return new Buffer(this, count, bufferData);
            }
        }

        private void uploadData(Buffer buffer, Consumer<ByteBuffer> bufferConsumer) {
            BufferUtils.uploadData(this.device, this.physicalDevice, this.allocator, this.commandPool,
                    this.commandBatches, this.queues, buffer, bufferConsumer);
        }

        public void uploadVertices(Buffer buffer, Vertex[] vertices) {
            this.uploadData(buffer, byteBuffer -> {
                for (Vertex vertex : vertices) {
                    vertex.write(byteBuffer);
                }
            });
        }

        public void uploadIndices(Buffer buffer, short[] indices) {
            this.uploadData(buffer, byteBuffer -> {
                for (short index : indices) {
                    byteBuffer.putShort(index);
                }
            });
        }
    }
}
