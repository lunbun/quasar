package io.github.lunbun.pulsar.component.pipeline;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public final class RenderPass {
    public final long renderPass;

    protected RenderPass(long renderPass) {
        this.renderPass = renderPass;
    }

    public static final class Builder {
        public final SwapChain swapChain;
        public final LogicalDevice device;

        public Builder(LogicalDevice device, SwapChain swapChain) {
            this.device = device;
            this.swapChain = swapChain;
        }

        public RenderPass createRenderPass() {
            long handle = this.createVkRenderPass();
            return new RenderPass(handle);
        }

        private long createVkRenderPass() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.callocStack(1, stack);
                colorAttachment.format(this.swapChain.imageFormat);
                colorAttachment.samples(VK10.VK_SAMPLE_COUNT_1_BIT);
                colorAttachment.loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR);
                colorAttachment.storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachment.stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                colorAttachment.stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
                colorAttachment.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
                colorAttachment.finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

                VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.callocStack(1, stack);
                colorAttachmentRef.attachment(0);
                colorAttachmentRef.layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

                VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
                subpass.pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS);
                subpass.colorAttachmentCount(1);
                subpass.pColorAttachments(colorAttachmentRef);

                VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
                renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
                renderPassInfo.pAttachments(colorAttachment);
                renderPassInfo.pSubpasses(subpass);

                LongBuffer pRenderPass = stack.mallocLong(1);

                if (VK10.vkCreateRenderPass(this.device.device, renderPassInfo, null, pRenderPass) != VK10.VK_NULL_HANDLE) {
                    throw new RuntimeException("Failed to create render pass!");
                }

                return pRenderPass.get(0);
            }
        }
    }
}
