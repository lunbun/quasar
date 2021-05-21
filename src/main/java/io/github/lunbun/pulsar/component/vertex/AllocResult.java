package io.github.lunbun.pulsar.component.vertex;

public final class AllocResult {
    public final long heap;
    public final int pointer;

    protected AllocResult(long heap, int pointer) {
        this.heap = heap;
        this.pointer = pointer;
    }
}
