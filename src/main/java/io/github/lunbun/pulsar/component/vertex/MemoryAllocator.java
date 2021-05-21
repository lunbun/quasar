package io.github.lunbun.pulsar.component.vertex;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.util.misc.MathUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.LongBuffer;
import java.util.List;
import java.util.Map;

// TODO: more optimized allocator
public final class MemoryAllocator {
    // 4 megabytes/slot
    private static final int CELL_SIZE = 512; // # of bytes/cell
    private static final int HEAP_SIZE = 8192; // # of cells

    private final LogicalDevice device;
    private final Map<Integer, Typed> typedAllocators;

    public MemoryAllocator(LogicalDevice device) {
        this.device = device;
        this.typedAllocators = new Int2ObjectOpenHashMap<>();
    }

    public AllocResult mallocAligned(int memoryType, int size, int alignment) {
        this.typedAllocators.computeIfAbsent(memoryType, type -> new Typed(this.device, type, HEAP_SIZE, CELL_SIZE));
        return this.typedAllocators.get(memoryType).mallocAligned(size, alignment);
    }

    public AllocResult malloc(int memoryType, int size) {
        this.typedAllocators.computeIfAbsent(memoryType, type -> new Typed(this.device, type, HEAP_SIZE, CELL_SIZE));
        return this.typedAllocators.get(memoryType).mallocAligned(size, 1);
    }

    public void free(long heap, int memoryType, int pointer, int size) {
        this.typedAllocators.get(memoryType).free(heap, pointer, size);
    }

    public void destroy() {
        for (Typed typedAllocator : this.typedAllocators.values()) {
            typedAllocator.destroy();
        }
    }

    private static final class MemorySlot {
        // pack each boolean tightly into a byte to avoid using any extra memory
        private final byte[] headers;
        public long heap;

        public MemorySlot(byte[] headers, long heap) {
            this.headers = headers;
            this.heap = heap;
        }
    }

    private static final class Typed {
        private final int cellSize;
        private final LogicalDevice device;
        private final int memoryType;
        private final int heapCellSize;

        private final List<MemorySlot> slots;

        public Typed(LogicalDevice device, int memoryType, int heapCellSize, int cellSize) {
            this.cellSize = cellSize;
            this.memoryType = memoryType;
            this.heapCellSize = heapCellSize;

            if ((heapCellSize & 7) != 0) {
                throw new RuntimeException("Heap must be a multiple of 8!");
            }

            this.device = device;

            this.slots = new ObjectArrayList<>();
            this.createSlot();
        }

        public void destroy() {
            for (MemorySlot slot : this.slots) {
                VK10.vkFreeMemory(this.device.device, slot.heap, null);
            }
        }

        public long getMemorySize() {
            return (long) this.slots.size() * this.heapCellSize * this.cellSize;
        }

        private MemorySlot createSlot() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                byte[] headers = new byte[this.heapCellSize >> 3];

                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
                allocInfo.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
                allocInfo.allocationSize((long) this.heapCellSize * this.cellSize);
                allocInfo.memoryTypeIndex(this.memoryType);

                LongBuffer pMemoryHeap = stack.mallocLong(1);

                if (VK10.vkAllocateMemory(this.device.device, allocInfo, null, pMemoryHeap) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate buffer memory!");
                }

                long memoryHeap = pMemoryHeap.get(0);

                MemorySlot slot = new MemorySlot(headers, memoryHeap);
                this.slots.add(slot);

                return slot;
            }
        }

        private boolean isEmpty(MemorySlot slot, int cellIndex) {
            byte header = slot.headers[cellIndex >> 3];
            return ((header >> (cellIndex & 7)) & 1) == 0;
        }

        private void claim(MemorySlot slot, int index) {
            slot.headers[index >> 3] |= (1 << (index & 7));
        }

        private void unclaim(MemorySlot slot, int index) {
            slot.headers[index >> 3] &= ~(1 << (index & 7));
        }

        // TODO: batch claim/free
        private void claim(MemorySlot slot, int index, int size) {
            for (int i = 0; i < size; ++i) {
                this.claim(slot, i + index);
            }
        }

        public void free(long heap, int pointer, int size) {
            for (MemorySlot slot : this.slots) {
                if (slot.heap == heap) {
                    int cellMemorySize = MathUtils.ceilIntDivide(size, this.cellSize);
                    int cellPointer = pointer / this.cellSize;
                    for (int i = 0; i < cellMemorySize; ++i) {
                        this.unclaim(slot, i + cellPointer);
                    }
                    return;
                }
            }
            throw new RuntimeException("Invalid heap");
        }

        public AllocResult mallocAligned(int size, int alignment) {
            int cellMemorySize = MathUtils.ceilIntDivide(size, this.cellSize);
            if (cellMemorySize >= this.heapCellSize) {
                throw new RuntimeException("Attempted to allocate " + size + ", more than slot limit of " + (this.heapCellSize * this.cellSize));
            }

            for (MemorySlot slot : this.slots) {
                for (int i = 0; i < this.heapCellSize; ++i) {
                    // if all headers in a byte are claimed, we can ignore those 8 headers
                    if ((i & 7) == 0 && (slot.headers[i >> 3] & 0xff) == 0xff) {
                        i += 8;
                        continue;
                    }

                    // check if there is even enough slots left in our search to fit the requested size
                    if (i >= this.heapCellSize - cellMemorySize) {
                        break;
                    }

                    // search for empty cells
                    if (this.isEmpty(slot, i) && ((i * this.cellSize) % alignment == 0)) {
                        boolean empty = true;

                        for (int j = 1; j < cellMemorySize; ++j) {
                            if (i + j >= this.heapCellSize) {
                                break;
                            }

                            empty = this.isEmpty(slot, i + j);

                            if (!empty) {
                                break;
                            }
                        }

                        if (empty) {
                            this.claim(slot, i, cellMemorySize);
                            return new AllocResult(slot.heap, i * this.cellSize);
                        } else {
                            i += cellMemorySize;
                        }
                    }
                }
            }

            // if we have to create a new slot, it is guaranteed that the first header is empty
            MemorySlot slot = this.createSlot();
            this.claim(slot, 0, cellMemorySize);
            PulsarApplication.LOGGER.info("Had to create a new memory slot! Memory type now has " +
                    (this.getMemorySize() / 1048576) + " MB");
            return new AllocResult(slot.heap, 0);
        }
    }
}
