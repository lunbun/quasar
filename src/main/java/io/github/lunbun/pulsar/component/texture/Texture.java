package io.github.lunbun.pulsar.component.texture;

import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.presentation.ImageViewsManager;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.component.vertex.MemoryAllocator;
import io.github.lunbun.pulsar.struct.texture.ImageData;
import io.github.lunbun.pulsar.struct.vertex.BufferData;
import io.github.lunbun.pulsar.util.vulkan.ImageUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Texture extends ImageData {
    private final long imageView;
    private final Loader loader;

    protected Texture(BufferData bufferData, long imageView, Loader loader) {
        super(bufferData);
        this.imageView = imageView;
        this.loader = loader;
    }

    public void destroy() {
        this.loader.destroy(this);
    }

    public static final class Loader {
        private final LogicalDevice device;
        private final PhysicalDevice physicalDevice;
        private final MemoryAllocator memoryAllocator;
        private final CommandPool commandPool;
        private final QueueManager queues;

        public Loader(LogicalDevice device, PhysicalDevice physicalDevice, MemoryAllocator memoryAllocator,
                      CommandPool commandPool, QueueManager queues) {
            this.device = device;
            this.physicalDevice = physicalDevice;
            this.memoryAllocator = memoryAllocator;
            this.commandPool = commandPool;
            this.queues = queues;
        }

        public void destroy(Texture texture) {
            ImageViewsManager.destroyImageView(this.device, texture.imageView);
            ImageUtils.destroyImage(this.device, this.memoryAllocator, texture);
        }

        public Texture loadPixels(ByteBuffer pixels, int imageSize, int texWidth, int texHeight, int format) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ImageData imageData = ImageUtils.createImage(this.device, this.physicalDevice, this.memoryAllocator,
                        imageSize, texWidth, texHeight, format, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack);
                ImageUtils.uploadPixels(this.device, this.physicalDevice, this.memoryAllocator, this.commandPool,
                        this.queues, texWidth, texHeight, imageSize, format, imageData, pixels, stack, true);
                long imageView = ImageViewsManager.createImageView(this.device, imageData.buffer, format);
                return new Texture(imageData, imageView, this);
            }
        }

        public Texture loadFile(String path) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (path.startsWith("file:/")) {
                    path = path.substring(6);
                }

                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                IntBuffer pChannels = stack.mallocInt(1);
                int channels = STBImage.STBI_rgb_alpha;
                ByteBuffer pixels = STBImage.stbi_load(path, pWidth, pHeight, pChannels, channels);
                int imageSize = pWidth.get(0) * pHeight.get(0) * channels;

                if (pixels == null) {
                    throw new RuntimeException("Failed to load texture image! " + STBImage.stbi_failure_reason());
                }

                Texture texture = loadPixels(pixels, imageSize, pWidth.get(0), pHeight.get(0),
                        VK10.VK_FORMAT_R8G8B8A8_SRGB);
                STBImage.stbi_image_free(pixels);

                return texture;
            }
        }
    }
}
