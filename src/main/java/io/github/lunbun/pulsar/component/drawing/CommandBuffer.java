package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.vertex.VertexBuffer;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public final class CommandBuffer {
    public final VkCommandBuffer buffer;

    private boolean isRecording = false;
    private boolean inRenderPass = false;

    protected CommandBuffer(VkCommandBuffer buffer) {
        this.buffer = buffer;
    }

    private void assertRecording() {
        if (!this.isRecording) {
            throw new RuntimeException("Not recording command buffer!");
        }
    }

    private void assertRenderPass() {
        this.assertRecording();

        if (!this.inRenderPass) {
            throw new RuntimeException("Not in render pass!");
        }
    }

    public void startRecording(CommandBatch batch) {
        if (VK10.vkBeginCommandBuffer(this.buffer, batch.beginInfo) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer!");
        }
        this.isRecording = true;
    }

    public void startRenderPass(RenderPass renderPass, Framebuffer framebuffer, CommandBatch batch) {
        this.assertRecording();

        batch.renderPassInfo.renderPass(renderPass.renderPass);

        VkClearValue.Buffer clearValues = VkClearValue.callocStack(1, batch.stack);
        clearValues.color().float32(batch.stack.floats(0, 0, 0, 1));
        batch.renderPassInfo.pClearValues(clearValues);

        batch.renderPassInfo.framebuffer(framebuffer.framebuffer);

        VK10.vkCmdBeginRenderPass(this.buffer, batch.renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);

        this.inRenderPass = true;
    }

    public void bindVertexBuffer(VertexBuffer vertexBuffer, CommandBatch batch) {
        this.assertRenderPass();
        // TODO: bind multiple buffers
        LongBuffer pBuffers = batch.stack.longs(vertexBuffer.buffer);
        LongBuffer pOffsets = batch.stack.longs(0);
        VK10.vkCmdBindVertexBuffers(this.buffer, 0, pBuffers, pOffsets);
    }

    public void bindPipeline(GraphicsPipeline graphicsPipeline) {
        this.assertRenderPass();
        VK10.vkCmdBindPipeline(this.buffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.pipeline);
    }

    public void draw(int count, int instanceCount, int first, int firstInstance) {
        this.assertRenderPass();
        VK10.vkCmdDraw(this.buffer, count, instanceCount, first, firstInstance);
    }

    public void endRenderPass() {
        this.assertRenderPass();
        VK10.vkCmdEndRenderPass(this.buffer);
        this.inRenderPass = false;
    }

    public void endRecording() {
        this.assertRecording();
        if (VK10.vkEndCommandBuffer(this.buffer) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer!");
        }
        this.isRecording = false;
    }
}
