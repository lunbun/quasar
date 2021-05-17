package io.github.lunbun.pulsar.component.drawing;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.SwapChainManager;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.struct.drawing.Frame;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.util.misc.CommandBufferRecorder;
import io.github.lunbun.pulsar.util.misc.MathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public final class FrameSynchronizer {
    public static final int MAX_FRAMES_IN_FLIGHT = 2;

    public final LogicalDevice device;
    public final SwapChain swapChain;
    public final SwapChainManager swapChainManager;
    public final QueueManager queues;
    public final CommandPool commandPool;
    public final BlockingTimer.Builder timings;
    public boolean framebufferResized;
    public CommandBufferRecorder bufferRecorder;

    private List<Frame> frames;
    private List<Frame> framesInFlight;
    private int currentFrame;

    public FrameSynchronizer(LogicalDevice device, SwapChain swapChain, SwapChainManager swapChainManager, QueueManager queues, CommandPool commandPool, BlockingTimer.Builder timings) {
        this.device = device;
        this.swapChain = swapChain;
        this.swapChainManager = swapChainManager;
        this.queues = queues;
        this.commandPool = commandPool;
        this.timings = timings;
        this.framebufferResized = false;
    }

    public void init() {
        this.frames = new ObjectArrayList<>();
        this.framesInFlight = new ObjectArrayList<>();

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            this.frames.add(new Frame(
                    this.timings.createTiming(BlockingTimer.Type.SEMAPHORE),
                    this.timings.createTiming(BlockingTimer.Type.SEMAPHORE),
                    this.timings.createTiming(BlockingTimer.Type.FENCE),
                    this.commandPool
            ));
        }

        for (int i = 0; i < this.swapChain.images.size(); ++i) {
            this.framesInFlight.add(null);
        }

        this.currentFrame = 0;
    }

    public void freeBuffers() {
        for (Frame frame : this.frames) {
            this.commandPool.freeBuffer(frame.commandBuffer);
            frame.commandBuffer = null;
        }
    }

    public void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Frame frame = this.frames.get(this.currentFrame);
            VK10.vkWaitForFences(this.device.device, frame.fence.handle, true, MathUtils.UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);
            int result = KHRSwapchain.vkAcquireNextImageKHR(this.device.device, this.swapChain.swapChain, MathUtils.UINT64_MAX,
                    frame.imageAvailable.handle, VK10.VK_NULL_HANDLE, pImageIndex);
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                this.swapChainManager.recreate();
                return;
            } else if (result != VK10.VK_SUCCESS && result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Failed to acquire swap chain image!");
            }
            int imageIndex = pImageIndex.get(0);

            if (this.bufferRecorder != null) {
                // TODO: reset the buffer instead of free & allocate
                if (frame.commandBuffer != null) {
                    this.commandPool.freeBuffer(frame.commandBuffer);
                }
                frame.commandBuffer = this.commandPool.allocateBuffer();
                this.bufferRecorder.record(frame.commandBuffer, imageIndex, this.currentFrame);
            }

            if (this.framesInFlight.get(imageIndex) != null) {
                VK10.vkWaitForFences(this.device.device, this.framesInFlight.get(imageIndex).fence.handle, true, MathUtils.UINT64_MAX);
            }
            this.framesInFlight.set(imageIndex, frame);

            LongBuffer waitSemaphores = frame.pImageAvailableSemaphore();
            LongBuffer signalSemaphores = frame.pRenderFinishedSemaphore();

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(waitSemaphores);
            submitInfo.pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(frame.commandBuffer.buffer));
            submitInfo.pSignalSemaphores(signalSemaphores);

            VK10.vkResetFences(this.device.device, frame.fence.handle);

            if (VK10.vkQueueSubmit(this.queues.getQueue(QueueFamily.GRAPHICS), submitInfo, frame.fence.handle) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer!");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signalSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(this.swapChain.swapChain));
            presentInfo.pImageIndices(pImageIndex);

            result = KHRSwapchain.vkQueuePresentKHR(this.queues.getQueue(QueueFamily.PRESENT), presentInfo);
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || result == KHRSwapchain.VK_SUBOPTIMAL_KHR || this.framebufferResized) {
                this.framebufferResized = false;
                this.swapChainManager.recreate();
            } else if (result != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image!");
            }

            this.currentFrame = (this.currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    public void endLoop() {
        this.device.waitIdle();
    }
}
