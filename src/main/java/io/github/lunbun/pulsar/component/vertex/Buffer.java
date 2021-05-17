package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.component.uniform.Uniform;
import io.github.lunbun.pulsar.struct.vertex.BufferData;
import io.github.lunbun.pulsar.util.vulkan.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class Buffer extends BufferData {
    public int count;
    public boolean useStagingUploads;

    private final Builder builder;

    protected Buffer(Builder builder, int count, boolean useStagingUploads, BufferData bufferData) {
        super(bufferData);
        this.builder = builder;
        this.count = count;
        this.useStagingUploads = useStagingUploads;
    }

    public void destroy() {
        this.builder.destroy(this);
    }

    public enum Type {
        UNIFORM(VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
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
        private final QueueManager queues;
        private final MemoryAllocator allocator;

        public Builder(LogicalDevice device, PhysicalDevice physicalDevice, CommandPool commandPool, QueueManager queues, MemoryAllocator allocator) {
            this.device = device;
            this.physicalDevice = physicalDevice;
            this.commandPool = commandPool;
            this.queues = queues;
            this.allocator = allocator;
        }

        protected void destroy(Buffer buffer) {
            BufferUtils.destroy(this.device, this.allocator, buffer);
        }

        public Buffer createBuffer(Type type, int count, long size, boolean useStagingUploads) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                BufferData bufferData;
                if (useStagingUploads) {
                    bufferData = BufferUtils.createBuffer(this.device, this.physicalDevice, this.allocator, size,
                            VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | type.usage,
                            VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack);
                } else {
                    bufferData = BufferUtils.createBuffer(this.device, this.physicalDevice, this.allocator, size,
                            type.usage, VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                            stack);
                }
                return new Buffer(this, count, useStagingUploads, bufferData);
            }
        }

        public Buffer createVertexBuffer(int count, long size, boolean useStagingUploads) {
            return this.createBuffer(Type.VERTEX, count, size, useStagingUploads);
        }

        public Buffer createIndexBuffer(int count, boolean useStagingUploads) {
            return this.createBuffer(Type.INDEX, count, (long) count * Short.BYTES, useStagingUploads);
        }

        public Buffer createUniformBuffer(long size, boolean useStagingUploads) {
            return this.createBuffer(Type.UNIFORM, -1, size, useStagingUploads);
        }

        private void uploadData(Buffer buffer, Consumer<ByteBuffer> bufferConsumer) {
            BufferUtils.uploadData(this.device, this.physicalDevice, this.allocator, this.commandPool, this.queues,
                    buffer, bufferConsumer, buffer.useStagingUploads);
        }

        public void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer) {
            this.uploadData(buffer, bufferCopy -> {
                bufferCopy.put(byteBuffer);
                bufferCopy.rewind();
            });
        }

        public void uploadUniform(Buffer buffer, Uniform uniform) {
            this.uploadData(buffer, byteBuffer -> {
                uniform.write(byteBuffer);
                byteBuffer.rewind();
            });
        }

        public void uploadVertices(Buffer buffer, Iterable<Vertex> vertices) {
            this.uploadData(buffer, byteBuffer -> {
                for (Vertex vertex : vertices) {
                    vertex.write(byteBuffer);
                }
                byteBuffer.rewind();
            });
        }

        public void uploadVertices(Buffer buffer, Vertex[] vertices) {
            this.uploadData(buffer, byteBuffer -> {
                for (Vertex vertex : vertices) {
                    vertex.write(byteBuffer);
                }
                byteBuffer.rewind();
            });
        }

        public void uploadIndices(Buffer buffer, Iterable<Short> indices) {
            this.uploadData(buffer, byteBuffer -> {
                for (short index : indices) {
                    byteBuffer.putShort(index);
                }
                byteBuffer.rewind();
            });
        }

        public void uploadIndices(Buffer buffer, short[] indices) {
            this.uploadData(buffer, byteBuffer -> {
                for (short index : indices) {
                    byteBuffer.putShort(index);
                }
                byteBuffer.rewind();
            });
        }
    }
}
