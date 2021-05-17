package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.util.vulkan.DataType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.ByteBuffer;
import java.util.List;

public final class Vertex {
    private final Object[] values;
    private final List<VertexData> data;

    protected Vertex(List<VertexData> data) {
        this.values = new Object[data.size()];
        this.data = data;
    }

    public void set(int index, Object value) {
        this.values[index] = value;
    }
    public void set(int offset, Object[] values) {
        System.arraycopy(values, 0, this.values, offset, values.length);
    }

    public void write(ByteBuffer buffer) {
        for (int i = 0; i < this.values.length; ++i) {
            this.data.get(i).type.writer.accept(buffer, this.values[i]);
        }
    }

    public static final class VertexData {
        public final DataType type;
        public final int offset;
        public final int location;

        public VertexData(DataType type, int offset, int location) {
            this.type = type;
            this.offset = offset;
            this.location = location;
        }
    }

    public static final class Builder {
        private final List<VertexData> data;
        private int offset;
        private boolean immutable;

        public Builder() {
            this.data = new ObjectArrayList<>();
            this.offset = 0;
            this.immutable = false;
        }

        public int sizeof() {
            return this.offset;
        }

        public void attribute(DataType vertexType, int location) {
            if (this.immutable) {
                throw new RuntimeException("Vertex builder is immutable! Once a vertex has been created, attributes cannot be added.");
            }
            this.data.add(new VertexData(vertexType, this.offset, location));
            this.offset += vertexType.size;
        }

        public Vertex createVertex() {
            this.immutable = true;
            return new Vertex(this.data);
        }

        public Vertex createVertex(Object... values) {
            Vertex vertex = this.createVertex();
            vertex.set(0, values);
            return vertex;
        }

        public VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack) {
            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.callocStack(1, stack);

            bindingDescription.binding(0);
            bindingDescription.stride(this.sizeof());
            bindingDescription.inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);

            return bindingDescription;
        }

        public VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(MemoryStack stack) {
            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.callocStack(this.data.size(), stack);

            for (int i = 0; i < this.data.size(); ++i) {
                VkVertexInputAttributeDescription description = attributeDescriptions.get(i);
                VertexData data = this.data.get(i);
                description.binding(0);
                description.location(data.location);
                description.format(data.type.format);
                description.offset(data.offset);
            }

            return attributeDescriptions;
        }
    }
}
