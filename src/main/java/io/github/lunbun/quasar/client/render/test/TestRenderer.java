package io.github.lunbun.quasar.client.render.test;

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
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import io.github.lunbun.quasar.client.render.VulkanRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

// based off of the vulkan tutorial
public final class TestRenderer implements VulkanRenderer {
    public PulsarApplication pulsar;
    public RenderPass renderPass;
    public GraphicsPipeline graphicsPipeline;
    public Shader shader;
    public Blend blendFunc;
    public DescriptorSetLayout descriptorSetLayout;
    public Vertex.Builder vertexBuilder;
    public Uniform uniform;
    public Matrix4f model;
    public Matrix4f view;
    public Matrix4f proj;
    public List<TestFrame> frames;
    public Mesh mesh;
    public Texture texture;
    public List<Framebuffer> framebuffers;

    @Override
    public void init(PulsarApplication pulsar) {
        this.pulsar = pulsar;

        this.vertexBuilder = new Vertex.Builder();
        this.vertexBuilder.attribute(DataType.VEC2, 0);
        this.vertexBuilder.attribute(DataType.VEC3, 1);
        Vertex[] vertices = new Vertex[] {
                this.vertexBuilder.createVertex(new Vector2f(-0.5f, -0.5f), new Vector3f(1.0f, 0.0f, 0.0f)),
                this.vertexBuilder.createVertex(new Vector2f(0.5f, -0.5f), new Vector3f(0.0f, 1.0f, 0.0f)),
                this.vertexBuilder.createVertex(new Vector2f(0.5f, 0.5f), new Vector3f(0.0f, 0.0f, 1.0f)),
                this.vertexBuilder.createVertex(new Vector2f(-0.5f, 0.5f), new Vector3f(1.0f, 1.0f, 1.0f))
        };
        short[] indices = new short[] { 0, 1, 2, 2, 3, 0 };

        Buffer vertexBuffer = pulsar.buffers.createVertexBuffer(4, 4L * this.vertexBuilder.sizeof(),
                true);
        Buffer indexBuffer = pulsar.buffers.createIndexBuffer(6, true);
        pulsar.buffers.uploadVertices(vertexBuffer, vertices);
        pulsar.buffers.uploadIndices(indexBuffer, indices);
        this.mesh = new Mesh(vertexBuffer, indexBuffer);

        this.framebuffers = new ObjectArrayList<>();

        Uniform.Builder uniformBuilder = new Uniform.Builder();
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        this.uniform = uniformBuilder.createUniform();
        this.model = new Matrix4f();
        this.view = new Matrix4f();
        this.proj = new Matrix4f();

        this.descriptorSetLayout = pulsar.descriptorSetLayouts.createDescriptorSetLayout();

        this.shader = new Shader("shaders/test.vert", "shaders/test.frag");
        this.blendFunc = new Blend(
                Blend.Factor.SRC_ALPHA, Blend.Operator.ADD, Blend.Factor.ONE_MINUS_SRC_ALPHA,
                Blend.Factor.ONE, Blend.Operator.ADD, Blend.Factor.ZERO
        );

        this.frames = new ObjectArrayList<>();
        for (int i = 0; i < PulsarApplication.MAX_FRAMES_IN_FLIGHT; ++i) {
            TestFrame frame = new TestFrame();
            this.frames.add(frame);
            frame.descriptorSet = pulsar.descriptorPool.allocateSet(this.descriptorSetLayout);
            frame.uniformBuffer = pulsar.buffers.createUniformBuffer(uniformBuilder.sizeof(), false);
            frame.descriptorSet.configure(frame.uniformBuffer);
        }

        this.texture = pulsar.textureLoader.loadFile(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("textures/texture.jpg"))
                        .toExternalForm());
    }

    @Override
    public void destroy() {
        this.texture.destroy();
        this.mesh.destroy();
        this.descriptorSetLayout.destroy();

        for (TestFrame frame : this.frames) {
            frame.uniformBuffer.destroy();
        }
    }

    @Override
    public void recreateFramebuffers() {
        this.renderPass = this.pulsar.renderPasses.createRenderPass();
        this.graphicsPipeline = this.pulsar.pipelines.createPipeline(this.shader, this.blendFunc,
                this.renderPass, this.descriptorSetLayout, this.vertexBuilder);
        this.pulsar.framebuffers.createFramebuffers(this.renderPass, this.framebuffers);
    }

    @Override
    public void destroyFramebuffers() {
        this.pulsar.framebuffers.destroy(this.framebuffers);
    }

    @Override
    public void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        Framebuffer framebuffer = this.framebuffers.get(framebufferIndex);
        TestFrame frame = this.frames.get(currentFrame);

        this.model.rotation((float) (GLFWWindow.getTime() * Math.toRadians(90)), 0, 0, 1);
        this.view.setLookAt(2, 2, 2, 0, 0, 0, 0, 0, 1);
        float aspectRatio = (float) this.pulsar.getSwapWidth() / this.pulsar.getSwapHeight();
        this.proj.setPerspective((float) Math.toRadians(45), aspectRatio, 0.1f, 10.0f);
        this.proj.m11(-this.proj.m11());
        this.uniform.set(0, this.model);
        this.uniform.set(1, this.view);
        this.uniform.set(2, this.proj);
        this.pulsar.buffers.uploadUniform(frame.uniformBuffer, this.uniform);

        buffer.startRenderPass(this.renderPass, framebuffer);
        buffer.bindPipeline(this.graphicsPipeline);
        buffer.bindDescriptorSet(this.graphicsPipeline, frame.descriptorSet);
        buffer.bindMesh(this.mesh);
        buffer.drawMesh(this.mesh, 1, 0, 0);
        buffer.endRenderPass();
    }
}
