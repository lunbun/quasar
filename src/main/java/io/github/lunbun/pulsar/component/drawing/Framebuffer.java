package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.pipeline.RenderPass;
import io.github.lunbun.pulsar.component.presentation.ImageViewsManager;
import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.List;

public final class Framebuffer {
    public final long framebuffer;

    protected Framebuffer(long framebuffer) {
        this.framebuffer = framebuffer;
    }

    protected void destroy(LogicalDevice device) {
        VK10.vkDestroyFramebuffer(device.device, this.framebuffer, null);
    }

    public static final class Builder {
        public final LogicalDevice device;
        public final SwapChain swapChain;
        public final ImageViewsManager imageViews;

        public Builder(LogicalDevice device, SwapChain swapChain, ImageViewsManager imageViews) {
            this.device = device;
            this.swapChain = swapChain;
            this.imageViews = imageViews;
        }

        private void createFramebuffer(long imageView, LongBuffer pAttachments, LongBuffer pFramebuffer, VkFramebufferCreateInfo framebufferInfo) {
            pAttachments.put(0, imageView);
            framebufferInfo.pAttachments(pAttachments);

            if (VK10.vkCreateFramebuffer(this.device.device, framebufferInfo, null, pFramebuffer) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }
        }

        public void destroy(List<Framebuffer> framebuffers) {
            for (int i = framebuffers.size() - 1; i >= 0; --i) {
                framebuffers.get(i).destroy(this.device);
                framebuffers.remove(i);
            }
        }

        public int getFramebufferCount() {
            return this.imageViews.imageViews.size();
        }

        public void createFramebuffers(RenderPass renderPass, List<Framebuffer> framebuffers) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer pAttachments = stack.mallocLong(1);
                LongBuffer pFramebuffer = stack.mallocLong(1);

                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
                framebufferInfo.sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass.renderPass);
                framebufferInfo.width(this.swapChain.extent.width());
                framebufferInfo.height(this.swapChain.extent.height());
                framebufferInfo.layers(1);

                for (long imageView : this.imageViews.imageViews) {
                    this.createFramebuffer(imageView, pAttachments, pFramebuffer, framebufferInfo);
                    framebuffers.add(new Framebuffer(pFramebuffer.get(0)));
                }
            }
        }
    }
}
