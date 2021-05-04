package io.github.lunbun.pulsar.struct.pipeline;

import io.github.lunbun.pulsar.util.shader.SPIRV;
import io.github.lunbun.pulsar.util.shader.ShaderType;

public final class ShaderModule {
    protected SPIRV spirv;
    protected long module;

    public final String path;
    public final ShaderType type;

    public ShaderModule(String path, ShaderType type) {
        this.path = path;
        this.type = type;
    }
}
