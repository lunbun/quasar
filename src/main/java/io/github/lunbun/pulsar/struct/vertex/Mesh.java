package io.github.lunbun.pulsar.struct.vertex;

import io.github.lunbun.pulsar.component.vertex.IndexBuffer;
import io.github.lunbun.pulsar.component.vertex.VertexBuffer;

public final class Mesh {
    // TODO: split single allocation into vertex and index buffers (see end of https://vulkan-tutorial.com/Vertex_buffers/Index_buffer)
    // TODO: allow meshes to have multiple vertex buffers
    public final VertexBuffer vertexBuffer;
    public final IndexBuffer indexBuffer;

    public Mesh(VertexBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = null;
    }

    public Mesh(VertexBuffer vertexBuffer, IndexBuffer indexBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
    }

    public void destroy() {
        this.vertexBuffer.destroy();
        if (this.indexBuffer != null) {
            this.indexBuffer.destroy();
        }
    }
}
