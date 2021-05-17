package io.github.lunbun.quasar.client.render.stage;

import io.github.lunbun.pulsar.component.uniform.DescriptorSet;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.struct.vertex.Mesh;

public class ImmediateFrame {
    public Mesh mesh;
    public Buffer uniformBuffer;
    public DescriptorSet descriptorSet;
}
