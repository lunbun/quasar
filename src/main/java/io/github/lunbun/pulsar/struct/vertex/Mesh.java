package io.github.lunbun.pulsar.struct.vertex;

import io.github.lunbun.pulsar.component.vertex.Buffer;

public final class Mesh {
    // TODO: split single allocation into vertex and index buffers (see end of https://vulkan-tutorial.com/Vertex_buffers/Index_buffer)
    // TODO: allow meshes to have multiple vertex buffers
    public final Buffer vertexBuffer;
    public final Buffer indexBuffer;

    public Mesh(Buffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = null;
    }

    public Mesh(Buffer vertexBuffer, Buffer indexBuffer) {
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
