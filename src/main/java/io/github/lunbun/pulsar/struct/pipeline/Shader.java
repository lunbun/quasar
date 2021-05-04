package io.github.lunbun.pulsar.struct.pipeline;

import io.github.lunbun.pulsar.component.pipeline.ShaderModule;
import io.github.lunbun.pulsar.util.shader.ShaderType;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;

public final class Shader {
    public final Map<ShaderType, ShaderModule> modules;

    public Shader(String vertPath, String fragPath) {
        this.modules = new Object2ObjectArrayMap<>();

        this.modules.put(ShaderType.VERTEX_SHADER, new ShaderModule(vertPath, ShaderType.VERTEX_SHADER));
        this.modules.put(ShaderType.FRAGMENT_SHADER, new ShaderModule(fragPath, ShaderType.FRAGMENT_SHADER));
    }

    public ShaderModule getVertex() {
        return this.modules.get(ShaderType.VERTEX_SHADER);
    }

    public ShaderModule getFragment() {
        return this.modules.get(ShaderType.FRAGMENT_SHADER);
    }
}
