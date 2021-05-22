package io.github.lunbun.pulsar.struct.uniform;

public abstract class DescriptorSetConfiguration {
    public final int binding;

    public DescriptorSetConfiguration(int binding) {
        this.binding = binding;
    }
}
