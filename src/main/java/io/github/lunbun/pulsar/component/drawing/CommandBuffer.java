package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.pipeline.GraphicsPipeline;
import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.uniform.DescriptorSet;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.struct.vertex.Mesh;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public final class CommandBuffer {
    public final VkCommandBuffer buffer;
    private final SwapChain swapChain;

    private boolean isRecording = false;
    private boolean inRenderPass = false;

    protected CommandBuffer(SwapChain swapChain, VkCommandBuffer buffer) {
        this.swapChain = swapChain;
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

    private void startRecording(int flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(flags);
            if (VK10.vkBeginCommandBuffer(this.buffer, beginInfo) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer!");
            }
            this.isRecording = true;
        }
    }

    public void startRecording() {
        this.startRecording(0);
    }

    public void startRecordingOneTimeSubmit() {
        this.startRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
    }

    public void startRenderPass(RenderPass renderPass, Framebuffer framebuffer) {
        this.assertRecording();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            VkRect2D renderArea = VkRect2D.callocStack(stack);
            renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
            renderArea.extent(this.swapChain.extent);
            renderPassInfo.renderArea(renderArea);

            renderPassInfo.renderPass(renderPass.renderPass);

            VkClearValue.Buffer clearValues = VkClearValue.callocStack(1, stack);
            clearValues.color().float32(stack.floats(0, 0, 0, 1));
            renderPassInfo.pClearValues(clearValues);

            renderPassInfo.framebuffer(framebuffer.framebuffer);

            VK10.vkCmdBeginRenderPass(this.buffer, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);
        }

        this.inRenderPass = true;
    }

    public void copyBuffer(long src, long dst, int size) {
        this.assertRecording();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // TODO: batch copy buffers
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.size(size);
            VK10.vkCmdCopyBuffer(this.buffer, src, dst, copyRegion);
        }
    }

    public void pipelineBarrier(VkImageMemoryBarrier.Buffer pBarriers, int srcStage, int dstStage) {
        this.assertRecording();
        VK10.vkCmdPipelineBarrier(this.buffer, srcStage, dstStage, 0, null,
                null, pBarriers);
    }

    public void copyBufferToImage(long buffer, long image, VkBufferImageCopy.Buffer pRegions) {
        this.assertRecording();
        // TODO: copying different regions
        VK10.vkCmdCopyBufferToImage(this.buffer, buffer, image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions);
    }

    public void bindPipeline(GraphicsPipeline graphicsPipeline) {
        this.assertRenderPass();
        VK10.vkCmdBindPipeline(this.buffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.pipeline);
    }

    public void bindMesh(Mesh mesh) {
        this.assertRenderPass();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffers = stack.longs(mesh.vertexBuffer.buffer);
            LongBuffer pOffsets = stack.longs(0);
            VK10.vkCmdBindVertexBuffers(this.buffer, 0, pBuffers, pOffsets);

            if (mesh.indexBuffer != null) {
                VK10.vkCmdBindIndexBuffer(this.buffer, mesh.indexBuffer.buffer, 0, VK10.VK_INDEX_TYPE_UINT16);
            }
        }
    }

    public void bindDescriptorSet(GraphicsPipeline pipeline, DescriptorSet descriptorSet) {
        this.assertRenderPass();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkCmdBindDescriptorSets(this.buffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipelineLayout,
                    0, stack.longs(descriptorSet.descriptorSet), null);
        }
    }

    public void drawMesh(Mesh mesh, int instanceCount, int first, int firstInstance) {
        this.assertRenderPass();
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

    public void endRecordingOneTimeSubmit(VkQueue queue, CommandPool commandPool) {
        this.assertRecording();
        if (VK10.vkEndCommandBuffer(this.buffer) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer!");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(this.buffer));

            VK10.vkQueueSubmit(queue, submitInfo, VK10.VK_NULL_HANDLE);
            VK10.vkQueueWaitIdle(queue);

            commandPool.freeBuffer(this);
        }

        this.isRecording = false;
    }
}
