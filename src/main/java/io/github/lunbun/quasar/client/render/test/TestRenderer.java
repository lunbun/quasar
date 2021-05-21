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
import io.github.lunbun.quasar.client.render.QuasarRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

// based off of the vulkan tutorial
public final class TestRenderer {
    public static RenderPass renderPass;
    public static GraphicsPipeline graphicsPipeline;
    public static Shader shader;
    public static Blend blendFunc;
    public static DescriptorSetLayout descriptorSetLayout;
    public static Vertex.Builder vertexBuilder;
    public static Uniform uniform;
    public static Matrix4f model;
    public static Matrix4f view;
    public static Matrix4f proj;
    public static List<TestFrame> frames;
    public static Mesh mesh;
    public static Texture texture;
    public static List<Framebuffer> framebuffers;

    public static void init() {
        vertexBuilder = new Vertex.Builder();
        vertexBuilder.attribute(DataType.VEC2, 0);
        vertexBuilder.attribute(DataType.VEC3, 1);
        Vertex[] vertices = new Vertex[] {
                vertexBuilder.createVertex(new Vector2f(-0.5f, -0.5f), new Vector3f(1.0f, 0.0f, 0.0f)),
                vertexBuilder.createVertex(new Vector2f(0.5f, -0.5f), new Vector3f(0.0f, 1.0f, 0.0f)),
                vertexBuilder.createVertex(new Vector2f(0.5f, 0.5f), new Vector3f(0.0f, 0.0f, 1.0f)),
                vertexBuilder.createVertex(new Vector2f(-0.5f, 0.5f), new Vector3f(1.0f, 1.0f, 1.0f))
        };
        short[] indices = new short[] { 0, 1, 2, 2, 3, 0 };

        Buffer vertexBuffer = QuasarRenderer.pulsar.buffers.createVertexBuffer(4, 4L * vertexBuilder.sizeof(),
                true);
        Buffer indexBuffer = QuasarRenderer.pulsar.buffers.createIndexBuffer(6, true);
        QuasarRenderer.pulsar.buffers.uploadVertices(vertexBuffer, vertices);
        QuasarRenderer.pulsar.buffers.uploadIndices(indexBuffer, indices);
        mesh = new Mesh(vertexBuffer, indexBuffer);

        framebuffers = new ObjectArrayList<>();

        Uniform.Builder uniformBuilder = new Uniform.Builder();
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        uniform = uniformBuilder.createUniform();
        model = new Matrix4f();
        view = new Matrix4f();
        proj = new Matrix4f();

        descriptorSetLayout = QuasarRenderer.pulsar.descriptorSetLayouts.createDescriptorSetLayout();

        shader = new Shader("shaders/test.vert", "shaders/test.frag");
        blendFunc = new Blend(
                Blend.Factor.SRC_ALPHA, Blend.Operator.ADD, Blend.Factor.ONE_MINUS_SRC_ALPHA,
                Blend.Factor.ONE, Blend.Operator.ADD, Blend.Factor.ZERO
        );

        frames = new ObjectArrayList<>();
        for (int i = 0; i < PulsarApplication.MAX_FRAMES_IN_FLIGHT; ++i) {
            TestFrame frame = new TestFrame();
            frames.add(frame);
            frame.descriptorSet = QuasarRenderer.pulsar.descriptorPool.allocateSet(descriptorSetLayout);
            frame.uniformBuffer = QuasarRenderer.pulsar.buffers.createUniformBuffer(uniformBuilder.sizeof(), false);
            frame.descriptorSet.configure(frame.uniformBuffer);
        }

        texture = QuasarRenderer.pulsar.textureLoader.loadFile(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("textures/texture.jpg"))
                        .toExternalForm());
    }

    public static void destroy() {
        texture.destroy();
        mesh.destroy();
        descriptorSetLayout.destroy();

        for (TestFrame frame : frames) {
            frame.uniformBuffer.destroy();
        }
    }

    public static void recreateFramebuffers() {
        renderPass = QuasarRenderer.pulsar.renderPasses.createRenderPass();
        graphicsPipeline = QuasarRenderer.pulsar.pipelines.createPipeline(shader, blendFunc,
                renderPass, descriptorSetLayout, vertexBuilder);
        QuasarRenderer.pulsar.framebuffers.createFramebuffers(renderPass, framebuffers);
    }

    public static void destroyFramebuffers() {
        QuasarRenderer.pulsar.framebuffers.destroy(framebuffers);
    }

    public static void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        Framebuffer framebuffer = framebuffers.get(framebufferIndex);
        TestFrame frame = frames.get(currentFrame);

        model.rotation((float) (GLFWWindow.getTime() * Math.toRadians(90)), 0, 0, 1);
        view.setLookAt(2, 2, 2, 0, 0, 0, 0, 0, 1);
        float aspectRatio = (float) QuasarRenderer.pulsar.getSwapWidth() / QuasarRenderer.pulsar.getSwapHeight();
        proj.setPerspective((float) Math.toRadians(45), aspectRatio, 0.1f, 10.0f);
        proj.m11(-proj.m11());
        uniform.set(0, model);
        uniform.set(1, view);
        uniform.set(2, proj);
        QuasarRenderer.pulsar.buffers.uploadUniform(frame.uniformBuffer, uniform);

        buffer.startRenderPass(renderPass, framebuffer);
        buffer.bindPipeline(graphicsPipeline);
        buffer.bindDescriptorSet(graphicsPipeline, frame.descriptorSet);
        buffer.bindMesh(mesh);
        buffer.drawMesh(mesh, 1, 0, 0);
        buffer.endRenderPass();
    }
}
