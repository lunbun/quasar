package io.github.lunbun.pulsar.struct.vertex;

public class BufferData {
    public long buffer;
    public int memoryType;
    public long memory;
    public int pointer;
    public long size;
    public int allocSize;

    protected BufferData(BufferData bufferData) {
        this.buffer = bufferData.buffer;
        this.memoryType = bufferData.memoryType;
        this.memory = bufferData.memory;
        this.pointer = bufferData.pointer;
        this.size = bufferData.size;
        this.allocSize = bufferData.allocSize;
    }

    public BufferData(long buffer, int memoryType, long memory, int pointer, long size, int allocSize) {
        this.buffer = buffer;
        this.memoryType = memoryType;
        this.memory = memory;
        this.pointer = pointer;
        this.size = size;
        this.allocSize = allocSize;
    }
}
