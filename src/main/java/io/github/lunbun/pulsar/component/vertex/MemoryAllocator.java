package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.util.misc.MathUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.LongBuffer;
import java.util.Map;

// TODO: more optimized allocator
public final class MemoryAllocator {
    // 64 kilobytes
    private static final int CELL_SIZE = 64;
    private static final int HEAP_SIZE = 1024;

    private final LogicalDevice device;
    private final Map<Integer, Typed> typedAllocators;

    public MemoryAllocator(LogicalDevice device) {
        this.device = device;
        this.typedAllocators = new Int2ObjectOpenHashMap<>();
    }

    public long heap(int memoryType) {
        this.typedAllocators.computeIfAbsent(memoryType, type -> new Typed(this.device, type, HEAP_SIZE, CELL_SIZE));
        return this.typedAllocators.get(memoryType).memoryHeap;
    }

    public int malloc(int memoryType, int size) {
        this.typedAllocators.computeIfAbsent(memoryType, type -> new Typed(this.device, type, HEAP_SIZE, CELL_SIZE));
        return this.typedAllocators.get(memoryType).malloc(size);
    }

    public void free(int memoryType, int pointer, int size) {
        this.typedAllocators.get(memoryType).free(pointer, size);
    }

    public void destroy() {
        for (Typed typedAllocator : this.typedAllocators.values()) {
            typedAllocator.destroy();
        }
    }

    private static final class Typed {
        private final int cellSize;
        private final LogicalDevice device;

        // pack each boolean tightly into a byte to avoid using any extra memory
        private final byte[] headers;
        public long memoryHeap;

        public Typed(LogicalDevice device, int memoryType, int heapCellSize, int cellSize) {
            this.cellSize = cellSize;

            if ((heapCellSize & 7) != 0) {
                throw new RuntimeException("Heap must be a multiple of 8!");
            }

            this.headers = new byte[heapCellSize >> 3];
            this.device = device;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
                allocInfo.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
                allocInfo.allocationSize((long) heapCellSize * cellSize);
                allocInfo.memoryTypeIndex(memoryType);

                LongBuffer pMemoryHeap = stack.mallocLong(1);

                if (VK10.vkAllocateMemory(device.device, allocInfo, null, pMemoryHeap) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate buffer memory!");
                }

                this.memoryHeap = pMemoryHeap.get(0);
            }
        }

        public void destroy() {
            VK10.vkFreeMemory(this.device.device, this.memoryHeap, null);
        }

        private boolean isEmpty(int cellIndex) {
            byte header = this.headers[cellIndex >> 3];
            return ((header >> (cellIndex & 7)) & 1) == 0;
        }

        private void claim(int index) {
            this.headers[index >> 3] |= (1 << (index & 7));
        }

        private void unclaim(int index) {
            this.headers[index >> 3] &= ~(1 << (index & 7));
        }

        // TODO: batch claim/free
        private void claim(int index, int size) {
            for (int i = 0; i < size; ++i) {
                this.claim(i + index);
            }
        }

        public void free(int pointer, int size) {
            int cellMemorySize = MathUtils.ceilIntDivide(size, this.cellSize);
            int cellPointer = pointer / this.cellSize;
            for (int i = 0; i < cellMemorySize; ++i) {
                this.unclaim(i + cellPointer);
            }
        }

        public int malloc(int size) {
            int cellMemorySize = MathUtils.ceilIntDivide(size, this.cellSize);
            int cellLength = this.headers.length << 3;
            for (int i = 0; i < cellLength; ++i) {
                // if all headers in a byte are claimed, we can ignore those 8 headers
                if ((i & 7) == 0 && (this.headers[i >> 3] & 0xff) == 0xff) {
                    i += 8;
                    continue;
                }

                // search for empty cells
                if (this.isEmpty(i)) {
                    boolean empty = true;

                    for (int j = 1; j < cellMemorySize; ++j) {
                        if (i + j >= cellLength) {
                            break;
                        }

                        empty = this.isEmpty(i + j);

                        if (!empty) {
                            break;
                        }
                    }

                    if (empty) {
                        this.claim(i, cellMemorySize);
                        return i * this.cellSize;
                    } else {
                        i += cellMemorySize;
                    }
                }
            }

            throw new RuntimeException("Failed to allocate memory!");
        }
    }
}
