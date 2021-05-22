package io.github.lunbun.pulsar.struct.uniform;

import io.github.lunbun.pulsar.util.shader.ShaderType;
import io.github.lunbun.pulsar.util.uniform.DescriptorSetType;

public final class LayoutData {
    public final int binding;
    public final DescriptorSetType type;
    public final ShaderType stage;

    public LayoutData(int binding, DescriptorSetType type, ShaderType stage) {
        this.binding = binding;
        this.type = type;
        this.stage = stage;
    }
}
