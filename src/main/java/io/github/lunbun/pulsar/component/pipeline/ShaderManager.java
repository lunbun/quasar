package io.github.lunbun.pulsar.component.pipeline;

import io.github.lunbun.pulsar.component.setup.LogicalDeviceManager;
import io.github.lunbun.pulsar.struct.pipeline.Shader;
import io.github.lunbun.pulsar.struct.pipeline.ShaderModule;
import io.github.lunbun.pulsar.util.shader.SPIRV;
import io.github.lunbun.pulsar.util.shader.ShaderUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.LongBuffer;

public final class ShaderManager {
    private static long createShaderModule(SPIRV spirv, LogicalDeviceManager logicalDevice, MemoryStack stack) {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
        createInfo.pCode(spirv.getBytecode());

        LongBuffer pShaderModule = stack.mallocLong(1);

        if (VK10.vkCreateShaderModule(logicalDevice.device, createInfo, null, pShaderModule) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to create shader module");
        }

        return pShaderModule.get(0);
    }

    public LogicalDeviceManager device;

    protected void compileModule(ShaderModule module, VkPipelineShaderStageCreateInfo shaderStageInfo, MemoryStack stack) {
        module.spirv = ShaderUtils.compileShaderFile(module.path, module.type);
        module.module = createShaderModule(module.spirv, this.device, stack);

        shaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        shaderStageInfo.stage(module.type.bits);
        shaderStageInfo.module(module.module);
        shaderStageInfo.pName(stack.UTF8("main"));
    }

    protected void destroyShader(Shader shader) {
        ShaderModule vertex = shader.getVertex();
        ShaderModule fragment = shader.getFragment();

        this.destroyModule(vertex.module);
        this.destroyModule(fragment.module);

        vertex.spirv.free();
        fragment.spirv.free();
    }

    private void destroyModule(long module) {
        VK10.vkDestroyShaderModule(this.device.device, module, null);
    }
}
