package io.github.lunbun.pulsar.component;

import io.github.lunbun.pulsar.util.PointerUtils;
import io.github.lunbun.pulsar.util.ValidationLayerUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public final class InstanceManager {
    public VkInstance instance;

    private PointerBuffer getRequiredExtensions() {
        PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();

        if (ValidationLayerUtils.ENABLE_VALIDATION_LAYERS) {
            MemoryStack stack = MemoryStack.stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
    }

    public void createInstance(String applicationName) {
        if (ValidationLayerUtils.ENABLE_VALIDATION_LAYERS && !ValidationLayerUtils.checkValidationLayerSupport()) {
            throw new RuntimeException("Validation layers requested, but not available!");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.callocStack(stack);
            appInfo.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(applicationName));
            appInfo.applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("No Engine"));
            appInfo.engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK10.VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.callocStack(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(this.getRequiredExtensions());

            if (ValidationLayerUtils.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(PointerUtils.asPointerBuffer(ValidationLayerUtils.VALIDATION_LAYERS));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);
                ValidationLayerUtils.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (VK10.vkCreateInstance(createInfo, null, instancePtr) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            this.instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    public void destroyInstance() {
        VK10.vkDestroyInstance(instance, null);
    }
}
