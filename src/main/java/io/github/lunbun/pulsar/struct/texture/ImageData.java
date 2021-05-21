package io.github.lunbun.pulsar.struct.texture;

import io.github.lunbun.pulsar.struct.vertex.BufferData;

public class ImageData extends BufferData {
    protected ImageData(BufferData bufferData) {
        super(bufferData);
    }

    public ImageData(long buffer, int memoryType, long memory, int pointer, long size, int allocSize) {
        super(buffer, memoryType, memory, pointer, size, allocSize);
    }
}
