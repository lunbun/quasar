package io.github.lunbun.pulsar.struct.uniform;

import io.github.lunbun.pulsar.component.texture.Texture;
import io.github.lunbun.pulsar.component.texture.TextureSampler;

public final class SamplerConfiguration extends DescriptorSetConfiguration {
    public final Texture texture;
    public final TextureSampler sampler;

    public SamplerConfiguration(Texture texture, TextureSampler textureSampler, int binding) {
        super(binding);
        this.texture = texture;
        this.sampler = textureSampler;
    }
}
