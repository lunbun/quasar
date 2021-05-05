package io.github.lunbun.pulsar.component.pipeline;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.struct.pipeline.ShaderModule;
import io.github.lunbun.pulsar.util.shader.SPIRV;
import io.github.lunbun.pulsar.util.shader.ShaderType;
import io.github.lunbun.pulsar.util.shader.ShaderUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.LongBuffer;
import java.util.Map;

public final class Shader {
    public final Map<ShaderType, ShaderModule> modules;

    protected Shader(ShaderModule vert, ShaderModule frag) {
        this.modules = new Object2ObjectArrayMap<>();

        this.modules.put(ShaderType.VERTEX_SHADER, vert);
        this.modules.put(ShaderType.FRAGMENT_SHADER, frag);
    }

    public Shader(String vertPath, String fragPath) {
        this(
                new ShaderModule(vertPath, ShaderType.VERTEX_SHADER),
                new ShaderModule(fragPath, ShaderType.FRAGMENT_SHADER)
        );
    }

    public ShaderModule getVertex() {
        return this.modules.get(ShaderType.VERTEX_SHADER);
    }

    public ShaderModule getFragment() {
        return this.modules.get(ShaderType.FRAGMENT_SHADER);
    }

    public static final class Builder {
        public final LogicalDevice device;

        public Builder(LogicalDevice device) {
            this.device = device;
        }

        public Shader createShader(String vertPath, String fragPath) {
            return new Shader(vertPath, fragPath);
        }

        private long createShaderModule(SPIRV spirv, MemoryStack stack) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirv.getBytecode());

            LongBuffer pShaderModule = stack.mallocLong(1);

            if (VK10.vkCreateShaderModule(this.device.device, createInfo, null, pShaderModule) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }

        protected void compileModule(ShaderModule module, VkPipelineShaderStageCreateInfo shaderStageInfo, MemoryStack stack) {
            module.spirv = ShaderUtils.compileShaderFile(module.path, module.type);
            module.module = createShaderModule(module.spirv, stack);

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
}
