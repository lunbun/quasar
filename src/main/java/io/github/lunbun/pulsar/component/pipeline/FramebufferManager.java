package io.github.lunbun.pulsar.component.pipeline;

import io.github.lunbun.pulsar.component.presentation.ImageViewsManager;
import io.github.lunbun.pulsar.component.presentation.SwapChain;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.List;

public final class FramebufferManager {
    public SwapChain swapChain;
    public RenderPass renderPass;
    public ImageViewsManager imageViews;

    public List<Long> framebuffers;

    public FramebufferManager() {
        this.framebuffers = new LongArrayList();
    }

    public void createFramebuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.framebuffers = new LongArrayList(this.imageViews.imageViews.size());

            LongBuffer pAttachments = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(this.renderPass.renderPass);
            framebufferInfo.width(this.swapChain.extent.width());
            framebufferInfo.height(this.swapChain.extent.height());
            framebufferInfo.layers(1);

            for (long imageView : this.imageViews.imageViews) {
                pAttachments.put(0, imageView);

                framebufferInfo.pAttachments(pAttachments);

            }
        }
    }
}
