package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.util.vulkan.AlignmentUtils;
import io.github.lunbun.pulsar.util.vulkan.DataType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.util.List;

// basically a copy of Vertex
public final class Uniform {
    private final Object[] values;
    private final List<DataType> uniforms;

    protected Uniform(List<DataType> uniforms) {
        this.values = new Object[uniforms.size()];
        this.uniforms = uniforms;
    }

    public void set(int index, Object value) {
        this.values[index] = value;
    }

    public void write(ByteBuffer buffer) {
        for (int i = 0; i < this.values.length; ++i) {
            buffer.position(AlignmentUtils.alignas(buffer.position(), this.uniforms.get(i).alignment));
            this.uniforms.get(i).writer.accept(buffer, this.values[i]);
        }
    }

    public static final class Builder {
        private final List<DataType> uniforms;
        private int offset;

        public Builder() {
            this.uniforms = new ObjectArrayList<>();
            this.offset = 0;
        }

        public int sizeof() {
            return this.offset;
        }

        public void uniform(DataType uniformType) {
            this.uniforms.add(uniformType);
            this.offset += uniformType.size;
            this.offset = AlignmentUtils.alignas(this.offset, uniformType.alignment);
        }

        public Uniform createUniform() {
            return new Uniform(this.uniforms);
        }
    }
}
