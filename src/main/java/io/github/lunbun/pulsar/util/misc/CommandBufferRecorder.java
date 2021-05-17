package io.github.lunbun.pulsar.util.misc;

import io.github.lunbun.pulsar.component.drawing.CommandBuffer;

@FunctionalInterface
public interface CommandBufferRecorder {
    void record(CommandBuffer commandBuffer, int index, int currentFrame);
}
