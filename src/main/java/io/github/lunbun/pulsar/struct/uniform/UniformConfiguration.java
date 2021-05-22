package io.github.lunbun.pulsar.struct.uniform;

import io.github.lunbun.pulsar.component.vertex.Buffer;

public final class UniformConfiguration extends DescriptorSetConfiguration {
    public final Buffer uniform;

    public UniformConfiguration(Buffer uniform, int binding) {
        super(binding);
        this.uniform = uniform;
    }
}
