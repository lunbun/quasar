package io.github.lunbun.pulsar.struct.drawing;

import io.github.lunbun.pulsar.component.drawing.BlockingTimer;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.pulsar.component.drawing.CommandPool;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

public class Frame {
    public final BlockingTimer imageAvailable;
    public final BlockingTimer renderFinished;
    public final BlockingTimer fence;
    public CommandBuffer commandBuffer;

    public Frame(BlockingTimer imageAvailable, BlockingTimer renderFinished, BlockingTimer fence, CommandPool commandPool) {
        this.imageAvailable = imageAvailable;
        this.renderFinished = renderFinished;
        this.fence = fence;
    }

    public LongBuffer pImageAvailableSemaphore() {
        return MemoryStack.stackGet().longs(this.imageAvailable.handle);
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return MemoryStack.stackGet().longs(this.renderFinished.handle);
    }

    public LongBuffer pFence() {
        return MemoryStack.stackGet().longs(this.fence.handle);
    }
}
