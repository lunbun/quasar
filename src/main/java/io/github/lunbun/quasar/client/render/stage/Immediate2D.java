package io.github.lunbun.quasar.client.render.stage;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.pipeline.Shader;
import io.github.lunbun.pulsar.component.uniform.DescriptorSetLayout;
import io.github.lunbun.pulsar.component.uniform.Uniform;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.component.vertex.Vertex;
import io.github.lunbun.pulsar.struct.pipeline.Blend;
import io.github.lunbun.pulsar.struct.vertex.Mesh;
import io.github.lunbun.pulsar.util.vulkan.DataType;
import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import io.github.lunbun.quasar.client.render.QuasarRenderer;
import io.github.lunbun.quasar.client.util.QuasarSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.List;

public final class Immediate2D {
    private static Shader shader;
    private static Blend blendFunc;
    private static DescriptorSetLayout descriptorSetLayout;
    private static Vertex.Builder vertexBuilder;
    private static Uniform uniform;
    private static RenderPass renderPass;
    private static GraphicsPipeline graphicsPipeline;
    private static final List<Framebuffer> framebuffers = new ObjectArrayList<>();

    private static Matrix4f model;
    private static Matrix4f view;
    private static Matrix4f proj;

    private static List<ImmediateFrame> frames;
    private static final List<Vertex> vertices = new ObjectArrayList<>();
    private static final ShortArrayList indices = new ShortArrayList();

    private Immediate2D() { }

    public static void clear() {
        vertices.clear();
        indices.clear();
    }

    private static short vertex(float x, float y, float r, float g, float b, float a) {
        vertices.add(vertexBuilder.createVertex(
                new Vector2f(x, y),
                new Vector4f(r, g, b, a)
        ));
        return (short) (vertices.size() - 1);
    }

    private static void index(short index) {
        indices.add(index);
    }

    public static void fill(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        short va = vertex(x1, y1, r, 0, 0, a);
        short vb = vertex(x2, y1, 0, g, 0, a);
        short vc = vertex(x2, y2, 0, 0, b, a);
        short vd = vertex(x1, y2, r, g, b, a);

        index(va);
        index(vb);
        index(vc);
        index(va);
        index(vc);
        index(vd);
    }

    private static void recreateFrame(int currentFrame) {
        ImmediateFrame frame = frames.get(currentFrame);
        if (frame.mesh != null) {
            frame.mesh.destroy();
        }
        Buffer vbo = QuasarRenderer.pulsar.buffers.createVertexBuffer(vertices.size(),
                (long) vertices.size() * vertexBuilder.sizeof(), QuasarSettings.USE_GUI_STAGING);
        QuasarRenderer.pulsar.buffers.uploadVertices(vbo, vertices);
        Buffer ibo = QuasarRenderer.pulsar.buffers.createIndexBuffer(indices.size(), QuasarSettings.USE_GUI_STAGING);
        QuasarRenderer.pulsar.buffers.uploadIndices(ibo, indices);

        model.rotation((float) (GLFWWindow.getTime() * Math.toRadians(90)), 0, 0, 1);
        view.setLookAt(2.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        float aspectRatio = (float) QuasarRenderer.pulsar.getSwapWidth() / QuasarRenderer.pulsar.getSwapHeight();
        proj.setPerspective((float) Math.toRadians(45), aspectRatio, 0.1f, 10);
        proj.m11(proj.m11() * -1);
        QuasarRenderer.pulsar.buffers.uploadUniform(frame.uniformBuffer, uniform);

        frame.mesh = new Mesh(vbo, ibo);
    }

    public static void destroy() {
        descriptorSetLayout.destroy();

        for (ImmediateFrame frame : frames) {
            frame.mesh.destroy();
            frame.uniformBuffer.destroy();
        }
    }

    public static void destroyFramebuffers() {
        QuasarRenderer.pulsar.framebuffers.destroy(framebuffers);
    }

    public static void recreateFramebuffers() {
        renderPass = QuasarRenderer.pulsar.renderPasses.createRenderPass();
        graphicsPipeline = QuasarRenderer.pulsar.pipelines.createPipeline(shader, blendFunc, renderPass, descriptorSetLayout, vertexBuilder);
        QuasarRenderer.pulsar.framebuffers.createFramebuffers(renderPass, framebuffers);
    }

    public static void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        Framebuffer framebuffer = framebuffers.get(framebufferIndex);

        recreateFrame(currentFrame);

        ImmediateFrame frame = frames.get(currentFrame);

        buffer.startRenderPass(renderPass, framebuffer);
        buffer.bindPipeline(graphicsPipeline);
        buffer.bindMesh(frame.mesh);
        buffer.bindDescriptorSet(graphicsPipeline, frame.descriptorSet);
        buffer.drawMesh(frame.mesh, 1, 0, 0);
        buffer.endRenderPass();
    }

    public static void initPulsar() {
        model = new Matrix4f();
        view = new Matrix4f();
        proj = new Matrix4f();

        vertexBuilder = new Vertex.Builder();
        vertexBuilder.attribute(DataType.VEC2, 0);
        vertexBuilder.attribute(DataType.VEC4, 1);

        Uniform.Builder uniformBuilder = new Uniform.Builder();
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        uniformBuilder.uniform(DataType.MAT4);
        uniform = uniformBuilder.createUniform();
        uniform.set(0, model);
        uniform.set(1, view);
        uniform.set(2, proj);

        descriptorSetLayout = QuasarRenderer.pulsar.descriptorSetLayouts.createDescriptorSetLayout();

        shader = new Shader("shader/shader.vert", "shader/shader.frag");
        blendFunc = new Blend(
                Blend.Factor.SRC_ALPHA, Blend.Operator.ADD, Blend.Factor.ONE_MINUS_SRC_ALPHA,
                Blend.Factor.ONE, Blend.Operator.ADD, Blend.Factor.ZERO
        );

        frames = new ObjectArrayList<>();
        for (int i = 0; i < PulsarApplication.MAX_FRAMES_IN_FLIGHT; ++i) {
            ImmediateFrame frame = new ImmediateFrame();
            frames.add(frame);
            frame.descriptorSet = QuasarRenderer.pulsar.descriptorPool.allocateSet(descriptorSetLayout);
            frame.uniformBuffer = QuasarRenderer.pulsar.buffers.createUniformBuffer(uniformBuilder.sizeof(), QuasarSettings.USE_GUI_STAGING);
            frame.descriptorSet.configure(frame.uniformBuffer);
        }
    }
}
