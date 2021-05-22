package io.github.lunbun.quasar.client.render.immediate;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.Shader;
import io.github.lunbun.pulsar.component.texture.Texture;
import io.github.lunbun.pulsar.component.uniform.DescriptorSetLayout;
import io.github.lunbun.pulsar.component.uniform.Uniform;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.component.vertex.Vertex;
import io.github.lunbun.pulsar.struct.pipeline.Blend;
import io.github.lunbun.pulsar.struct.vertex.Mesh;
import io.github.lunbun.pulsar.util.vulkan.DataType;
import io.github.lunbun.quasar.client.util.QuasarSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public enum Immediate {
    POSITION_COLOR(vertexBuilder -> {
        vertexBuilder.attribute(DataType.VEC3, 0);
        vertexBuilder.attribute(DataType.UINT, 1);
    }, new Shader("shaders/positionColor.vert", "shaders/positionColor.frag"), false);

    public static Immediate[] VALUES = Immediate.values();

    private final Consumer<Vertex.Builder> attributeWriter;
    private final boolean useIndexBuffer;
    private final Shader shader;
    private PulsarApplication pulsar;
    private Blend blendFunc;
    private DescriptorSetLayout descriptorSetLayout;
    private Vertex.Builder vertexBuilder;
    private Uniform uniform;
    private RenderPass renderPass;
    private GraphicsPipeline graphicsPipeline;
    private final List<Framebuffer> framebuffers = new ObjectArrayList<>();
    private List<ImmediateFrame> frames;
    private Matrix4f matrix;
    private List<Mesh> submittedMeshes;
    private Texture texture;

    private ByteBuffer vertices;
    private ByteBuffer indices;

    Immediate(Consumer<Vertex.Builder> attributeWriter, Shader shader, boolean useIndexBuffer) {
        this.attributeWriter = attributeWriter;
        this.shader = shader;
        this.useIndexBuffer = useIndexBuffer;
    }

    public void init(PulsarApplication pulsar) {
        this.pulsar = pulsar;

        // we don't actually use vertex builder to create vertices, we just use it to get the size of the vertex
        this.vertexBuilder = new Vertex.Builder();
        attributeWriter.accept(this.vertexBuilder);

        Uniform.Builder uniformBuilder = new Uniform.Builder();
        uniformBuilder.uniform(DataType.MAT4);
        this.uniform = uniformBuilder.createUniform();
        this.matrix = new Matrix4f();
        this.uniform.set(0, this.matrix);
        this.matrix.identity();
        this.matrix.m11(-this.matrix.m11());

        this.descriptorSetLayout = pulsar.descriptorSetLayouts.createDescriptorSetLayout();

        this.blendFunc = new Blend(
                Blend.Factor.SRC_ALPHA, Blend.Operator.ADD, Blend.Factor.ONE_MINUS_SRC_ALPHA,
                Blend.Factor.ONE, Blend.Operator.ADD, Blend.Factor.ZERO
        );

        this.frames = new ObjectArrayList<>();
        for (int i = 0; i < PulsarApplication.MAX_FRAMES_IN_FLIGHT; ++i) {
            ImmediateFrame frame = new ImmediateFrame();
            this.frames.add(frame);
            frame.descriptorSet = pulsar.descriptorPool.allocateSet(descriptorSetLayout);
            frame.uniformBuffer = pulsar.buffers.createUniformBuffer(uniformBuilder.sizeof(), true);
            pulsar.buffers.uploadUniform(frame.uniformBuffer, this.uniform);
            frame.descriptorSet.configure(frame.uniformBuffer);
        }

        this.submittedMeshes = new ObjectArrayList<>();
        this.vertices = ByteBuffer.allocate(0);
        this.indices = ByteBuffer.allocate(0);
        this.texture = pulsar.textureLoader.loadFile(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("textures/texture.jpg"))
                        .toExternalForm());
    }

    private void recreateFrame(int currentFrame) {
        ImmediateFrame frame = this.frames.get(currentFrame);

        for (Mesh mesh : frame.meshes) {
            mesh.destroy();
        }

        frame.meshes.clear();
        frame.meshes.addAll(this.submittedMeshes);
        this.submittedMeshes.clear();
    }

    public void submitMesh() {
        int vertexCount = this.vertices.capacity() / this.vertexBuilder.sizeof();
        Buffer vbo = this.pulsar.buffers.createVertexBuffer(vertexCount, this.vertices.capacity(),
                QuasarSettings.USE_IMMEDIATE_STAGING);
        this.pulsar.buffers.uploadBuffer(vbo, this.vertices);
        Buffer ibo = null;
        if (this.useIndexBuffer) {
            int indexCount = this.indices.capacity() / Short.BYTES;
            ibo = this.pulsar.buffers.createIndexBuffer(indexCount, QuasarSettings.USE_IMMEDIATE_STAGING);
            this.pulsar.buffers.uploadBuffer(ibo, this.indices);
        }

        this.submittedMeshes.add(new Mesh(vbo, ibo));

        this.vertices = ByteBuffer.allocate(0);
        this.indices = ByteBuffer.allocate(0);
    }

    public void destroy() {
        this.texture.destroy();
        this.descriptorSetLayout.destroy();

        for (ImmediateFrame frame : this.frames) {
            for (Mesh mesh : frame.meshes) {
                mesh.destroy();
            }
            frame.uniformBuffer.destroy();
        }
    }

    public void destroyFramebuffers() {
        this.pulsar.framebuffers.destroy(this.framebuffers);
    }

    public void recreateFramebuffers() {
        this.renderPass = this.pulsar.renderPasses.createRenderPass();
        this.graphicsPipeline = this.pulsar.pipelines.createPipeline(this.shader, this.blendFunc,
                this.renderPass, this.descriptorSetLayout, this.vertexBuilder);
        this.pulsar.framebuffers.createFramebuffers(this.renderPass, this.framebuffers);
    }

    public void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        Framebuffer framebuffer = this.framebuffers.get(framebufferIndex);

        recreateFrame(currentFrame);

        ImmediateFrame frame = this.frames.get(currentFrame);

        buffer.startRenderPass(this.renderPass, framebuffer);
        buffer.bindPipeline(this.graphicsPipeline);
        buffer.bindDescriptorSet(this.graphicsPipeline, frame.descriptorSet);
        for (Mesh mesh : frame.meshes) {
            buffer.bindMesh(mesh);
            buffer.drawMesh(mesh, 1, 0, 0);
        }
        buffer.endRenderPass();
    }

    public void addVertices(ByteBuffer buffer) {
        ByteBuffer previous = this.vertices;
        this.vertices = ByteBuffer.allocate(this.vertices.capacity() + buffer.capacity());
        this.vertices.put(previous);
        this.vertices.put(buffer);
        this.vertices.rewind();
    }

    public void addIndices(ByteBuffer buffer) {
        ByteBuffer previous = this.indices;
        this.indices = ByteBuffer.allocate(this.indices.capacity() + buffer.capacity());
        this.indices.put(previous);
        this.indices.put(buffer);
        this.indices.rewind();
    }
}
