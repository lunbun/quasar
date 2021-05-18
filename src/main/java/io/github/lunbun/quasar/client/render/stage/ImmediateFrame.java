package io.github.lunbun.quasar.client.render.stage;

import io.github.lunbun.pulsar.component.uniform.DescriptorSet;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.struct.vertex.Mesh;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class ImmediateFrame {
    public List<Mesh> meshes;
    public Buffer uniformBuffer;
    public DescriptorSet descriptorSet;

    public ImmediateFrame() {
        this.meshes = new ObjectArrayList<>();
    }
}
