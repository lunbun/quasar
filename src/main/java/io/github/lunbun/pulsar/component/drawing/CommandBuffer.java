package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.struct.vertex.Mesh;
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

    public void startRecording(CommandBatch batch, int flags) {
        batch.getBeginInfo().flags(flags);
        if (VK10.vkBeginCommandBuffer(this.buffer, batch.getBeginInfo()) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer!");
        }
        this.isRecording = true;
    }

    public void startRecording(CommandBatch batch) {
        this.startRecording(batch, 0);
    }

    public void startRenderPass(RenderPass renderPass, Framebuffer framebuffer, CommandBatch batch) {
        this.assertRecording();
        VkRenderPassBeginInfo renderPassInfo = batch.getRenderPassInfo();

        renderPassInfo.renderPass(renderPass.renderPass);

        VkClearValue.Buffer clearValues = VkClearValue.callocStack(1, batch.stack);
        clearValues.color().float32(batch.stack.floats(0, 0, 0, 1));
        renderPassInfo.pClearValues(clearValues);

        renderPassInfo.framebuffer(framebuffer.framebuffer);

        VK10.vkCmdBeginRenderPass(this.buffer, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);

        this.inRenderPass = true;
    }

    public void copyBuffer(long src, long dst, int size, CommandBatch batch) {
        this.assertRecording();
        // TODO: batch copy buffers
        VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, batch.stack);
        copyRegion.size(size);
        VK10.vkCmdCopyBuffer(this.buffer, src, dst, copyRegion);
    }

    public void bindPipeline(GraphicsPipeline graphicsPipeline) {
        this.assertRenderPass();
        VK10.vkCmdBindPipeline(this.buffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.pipeline);
    }

    public void bindMesh(Mesh mesh, CommandBatch batch) {
        this.assertRecording();
        LongBuffer pBuffers = batch.stack.longs(mesh.vertexBuffer.buffer);
        LongBuffer pOffsets = batch.stack.longs(0);
        VK10.vkCmdBindVertexBuffers(this.buffer, 0, pBuffers, pOffsets);

        if (mesh.indexBuffer != null) {
            VK10.vkCmdBindIndexBuffer(this.buffer, mesh.indexBuffer.buffer, 0, VK10.VK_INDEX_TYPE_UINT16);
        }
    }

    public void drawMesh(Mesh mesh, int instanceCount, int first, int firstInstance) {
        this.assertRecording();
        if (mesh.indexBuffer == null) {
            VK10.vkCmdDraw(this.buffer, mesh.vertexBuffer.count, instanceCount, first, firstInstance);
        } else {
            VK10.vkCmdDrawIndexed(this.buffer, mesh.indexBuffer.count, instanceCount, 0, first, firstInstance);
        }
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
