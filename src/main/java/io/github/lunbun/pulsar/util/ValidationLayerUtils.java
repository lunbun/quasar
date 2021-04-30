package io.github.lunbun.pulsar.util;

import io.github.lunbun.quasar.Quasar;
import io.github.lunbun.pulsar.component.InstanceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;

// https://github.com/Naitsirc98/Vulkan-Tutorial-Java/blob/ff0567a6635322d0413196f2ceffe338eef52bdb/src/main/java/javavulkantutorial/Ch02ValidationLayers.java
public final class ValidationLayerUtils {
    private static final Logger LOGGER = LogManager.getLogger(Quasar.MOD_NAME);

    public static final boolean ENABLE_VALIDATION_LAYERS = Configuration.DEBUG.get(true);
    private static long debugMessenger;

    public static final Set<String> VALIDATION_LAYERS;

    private ValidationLayerUtils() { }

    static {
        if(ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null;
        }
    }

    public static boolean checkValidationLayerSupport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            VK10.vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.mallocStack(layerCount.get(0), stack);
            VK10.vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            for (String layerName : VALIDATION_LAYERS) {
                boolean layerFound = false;

                for (int i = 0; i < availableLayers.capacity(); ++i) {
                    VkLayerProperties layerProperties = availableLayers.get(i);

                    if (layerName.equals(layerProperties.layerNameString())) {
                        layerFound = true;
                        break;
                    }
                }

                if (!layerFound) {
                    return false;
                }
            }

            return true;
        }
    }

    public static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(ValidationLayerUtils::debugCallback);
    }

    public static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        if (messageSeverity >= EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
            LOGGER.error(callbackData.pMessageString());
        } else if (messageSeverity >= EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
            LOGGER.warn(callbackData.pMessageString());
        }

        return VK10.VK_FALSE;
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {
        if (VK10.vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != MemoryUtil.NULL) {
            return EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks,
                    pDebugMessenger);
        }

        return VK10.VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger,
                                                      VkAllocationCallbacks allocationCallbacks) {
        if (VK10.vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != MemoryUtil.NULL) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }
    }

    public static void setupDebugMessenger(InstanceManager instance) {
        if (!ENABLE_VALIDATION_LAYERS) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK10.VK_NULL_HANDLE);

            if (createDebugUtilsMessengerEXT(instance.instance, createInfo, null, pDebugMessenger) !=
                    VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public static void destroy(InstanceManager instance) {
        if (ENABLE_VALIDATION_LAYERS) {
            destroyDebugUtilsMessengerEXT(instance.instance, debugMessenger, null);
        }
    }
}
