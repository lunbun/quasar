package io.github.lunbun.pulsar.util.vulkan;

import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.pulsar.component.drawing.CommandPool;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.component.setup.QueueManager;
import io.github.lunbun.pulsar.component.vertex.AllocResult;
import io.github.lunbun.pulsar.component.vertex.MemoryAllocator;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.struct.texture.ImageData;
import io.github.lunbun.pulsar.struct.vertex.BufferData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public final class ImageUtils {
    public static VkMemoryRequirements getMemoryRequirements(LogicalDevice device, long image, MemoryStack stack) {
        VkMemoryRequirements memRequirements = VkMemoryRequirements.callocStack(stack);
        VK10.vkGetImageMemoryRequirements(device.device, image, memRequirements);
        return memRequirements;
    }

    private static long createImage(LogicalDevice device, int texWidth, int texHeight, int format, MemoryStack stack) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
        imageInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK10.VK_IMAGE_TYPE_2D);
        imageInfo.extent(VkExtent3D.mallocStack(stack).set(texWidth, texHeight, 1));
        imageInfo.mipLevels(1);
        imageInfo.arrayLayers(1);
        imageInfo.format(format);
        imageInfo.tiling(VK10.VK_IMAGE_TILING_OPTIMAL);
        imageInfo.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        imageInfo.usage(VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT);
        imageInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
        imageInfo.samples(VK10.VK_SAMPLE_COUNT_1_BIT);

        LongBuffer pTextureImage = stack.mallocLong(1);

        if (VK10.vkCreateImage(device.device, imageInfo, null, pTextureImage) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to create image!");
        }

        return pTextureImage.get(0);
    }

    public static ImageData createImage(LogicalDevice device, PhysicalDevice physicalDevice, MemoryAllocator allocator,
                                        int imageSize, int texWidth, int texHeight, int format, int properties,
                                        MemoryStack stack) {
        long textureImage = createImage(device, texWidth, texHeight, format, stack);

        VkMemoryRequirements memoryRequirements = getMemoryRequirements(device, textureImage, stack);
        int memoryType = BufferUtils.findMemoryType(physicalDevice, memoryRequirements.memoryTypeBits(), properties);
        AllocResult allocResult = allocator.mallocAligned(memoryType, (int) memoryRequirements.size(), (int) memoryRequirements.alignment());
        long memory = allocResult.heap;
        int pointer = allocResult.pointer;

        VK10.vkBindImageMemory(device.device, textureImage, memory, pointer);
        return new ImageData(textureImage, memoryType, memory, pointer, imageSize, (int) memoryRequirements.size());
    }

    public static void transitionImageLayout(long image, int format, int oldLayout, int newLayout,
                                             CommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);

            VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.callocStack(stack);
            subresourceRange.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            subresourceRange.baseMipLevel(0);
            subresourceRange.levelCount(1);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(1);
            barrier.subresourceRange(subresourceRange);

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
                    newLayout == VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new UnsupportedOperationException("Unsupported layout transition!");
            }

            commandBuffer.pipelineBarrier(barrier, sourceStage, destinationStage);
        }
    }

    public static void copyBufferToImage(long buffer, long image, int width, int height, CommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);
            region.bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset(VkOffset3D.mallocStack(stack).set(0, 0, 0));
            region.imageExtent(VkExtent3D.mallocStack(stack).set(width, height, 1));

            commandBuffer.copyBufferToImage(buffer, image, region);
        }
    }

    public static void uploadPixels(LogicalDevice device, PhysicalDevice physicalDevice, MemoryAllocator allocator,
                                    CommandPool commandPool, QueueManager queues, int width, int height, int imageSize,
                                    int format, ImageData image, ByteBuffer pixels, MemoryStack stack,
                                    boolean useStagingBuffer) {
        if (useStagingBuffer) {
            BufferData stagingBuffer = BufferUtils.createBuffer(device, physicalDevice, allocator,
                    imageSize, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                            VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stack);

            CommandBuffer commandBuffer = commandPool.allocateBuffer();
            commandBuffer.startRecordingOneTimeSubmit();
            {
                // we're creating a staging buffer manually, so we don't want to stage the staging buffer
                BufferUtils.uploadData(device, physicalDevice, allocator, stagingBuffer, (bufferCopy) -> {
                    bufferCopy.put(pixels);
                    bufferCopy.rewind();
                    pixels.rewind();
                }, false, commandBuffer);

                transitionImageLayout(image.buffer, format, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                        VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, commandBuffer);
                copyBufferToImage(stagingBuffer.buffer, image.buffer, width, height, commandBuffer);
                transitionImageLayout(image.buffer, format, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, commandBuffer);
            }
            commandBuffer.endRecordingOneTimeSubmit(queues.getQueue(QueueFamily.GRAPHICS), commandPool);

            BufferUtils.destroy(device, allocator, stagingBuffer);
        } else {
            throw new UnsupportedOperationException("Staging buffers required for image uploading!");
        }
    }

    public static void destroyImage(LogicalDevice device, MemoryAllocator allocator, ImageData imageData) {
        VK10.vkDestroyImage(device.device, imageData.buffer, null);
        allocator.free(imageData.memory, imageData.memoryType, imageData.pointer, imageData.allocSize);
    }
}
