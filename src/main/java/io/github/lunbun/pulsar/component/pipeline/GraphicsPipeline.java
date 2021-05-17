package io.github.lunbun.pulsar.component.pipeline;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.uniform.DescriptorSetLayout;
import io.github.lunbun.pulsar.component.vertex.Vertex;
import io.github.lunbun.pulsar.struct.pipeline.Blend;
import io.github.lunbun.pulsar.struct.pipeline.ShaderModule;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public final class GraphicsPipeline {
    public long pipelineLayout;
    public long pipeline;
    public final RenderPass renderPass;

    protected GraphicsPipeline(RenderPass renderPass) {
        this.renderPass = renderPass;
    }

    public static final class Builder {
        private final List<GraphicsPipeline> pipelinePool;

        public final Shader.Builder shaders;
        public final SwapChain swapChain;
        public final LogicalDevice device;

        public Builder(LogicalDevice device, Shader.Builder shaders, SwapChain swapChain) {
            this.device = device;
            this.shaders = shaders;
            this.swapChain = swapChain;

            this.pipelinePool = new ObjectArrayList<>();
        }

        // TODO: multiple render passes
        public GraphicsPipeline createPipeline(Shader shader, Blend blendFunc, RenderPass renderPass, DescriptorSetLayout descriptorSetLayout, Vertex.Builder vertexBuilder) {
            GraphicsPipeline pipeline = new GraphicsPipeline(renderPass);
            this.createVkPipeline(pipeline, shader, blendFunc, renderPass, descriptorSetLayout, vertexBuilder);

            this.pipelinePool.add(pipeline);
            return pipeline;
        }

        public void destroy() {
            for (int i = this.pipelinePool.size() - 1; i >= 0; --i) {
                this.destroyPipeline(this.pipelinePool.get(i));
                this.pipelinePool.remove(i);
            }
        }

        private void destroyPipeline(GraphicsPipeline pipeline) {
            VK10.vkDestroyPipeline(this.device.device, pipeline.pipeline, null);
            VK10.vkDestroyPipelineLayout(this.device.device, pipeline.pipelineLayout, null);
        }

        private void createVkPipeline(GraphicsPipeline pipeline, Shader shader, Blend blend, RenderPass renderPass, DescriptorSetLayout descriptorSetLayout, Vertex.Builder vertexBuilder) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPipelineShaderStageCreateInfo.Buffer shaderStageInfos = VkPipelineShaderStageCreateInfo.callocStack(2, stack);

                ShaderModule vertex = shader.getVertex();
                ShaderModule fragment = shader.getFragment();

                this.shaders.compileModule(vertex, shaderStageInfos.get(0), stack);
                this.shaders.compileModule(fragment, shaderStageInfos.get(1), stack);

                VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack);
                vertexInputInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
                if (vertexBuilder != null) {
                    vertexInputInfo.pVertexBindingDescriptions(vertexBuilder.getBindingDescription(stack));
                    vertexInputInfo.pVertexAttributeDescriptions(vertexBuilder.getAttributeDescriptions(stack));
                }

                VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
                inputAssembly.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);

                VkViewport.Buffer viewport = VkViewport.callocStack(1, stack);
                viewport.x(0);
                viewport.y(0);
                viewport.width(this.swapChain.extent.width());
                viewport.height(this.swapChain.extent.height());
                viewport.minDepth(0);
                viewport.maxDepth(1);

                VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack);
                scissor.offset(VkOffset2D.callocStack(stack).set(0, 0));
                scissor.extent(this.swapChain.extent);

                VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack);
                viewportState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.pViewports(viewport);
                viewportState.pScissors(scissor);

                VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack);
                rasterizer.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);
                rasterizer.polygonMode(VK10.VK_POLYGON_MODE_FILL);
                rasterizer.lineWidth(1);
                rasterizer.cullMode(VK10.VK_CULL_MODE_NONE);
                rasterizer.frontFace(VK10.VK_FRONT_FACE_CLOCKWISE);
                rasterizer.depthBiasEnable(false);

                VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack);
                multisampling.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

                VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, stack);
                colorBlendAttachment.colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT |
                        VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
                if (blend == null) {
                    colorBlendAttachment.blendEnable(false);
                } else {
                    colorBlendAttachment.blendEnable(true);
                    colorBlendAttachment.srcColorBlendFactor(blend.srcColorBlendFactor.vk);
                    colorBlendAttachment.dstColorBlendFactor(blend.dstColorBlendFactor.vk);
                    colorBlendAttachment.colorBlendOp(blend.colorBlendOp.vk);
                    colorBlendAttachment.srcAlphaBlendFactor(blend.srcAlphaBlendFactor.vk);
                    colorBlendAttachment.dstAlphaBlendFactor(blend.dstAlphaBlendFactor.vk);
                    colorBlendAttachment.alphaBlendOp(blend.alphaBlendOp.vk);
                }

                VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack);
                colorBlending.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK10.VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachment);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

                //noinspection SuspiciousNameCombination
                IntBuffer dynamicStates = stack.ints(
                        VK10.VK_DYNAMIC_STATE_VIEWPORT,
                        VK10.VK_DYNAMIC_STATE_LINE_WIDTH
                );
                VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.callocStack(stack);
                dynamicState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
                dynamicState.pDynamicStates(dynamicStates);

                LongBuffer pPipelineLayout = stack.longs(VK10.VK_NULL_HANDLE);

                VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
                pipelineLayoutInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                if (descriptorSetLayout != null) {
                    pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout.layout));
                }

                if (VK10.vkCreatePipelineLayout(this.device.device, pipelineLayoutInfo, null, pPipelineLayout) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create pipeline layout!");
                }

                VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack);
                pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.pStages(shaderStageInfos);
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.layout(pPipelineLayout.get(0));
                pipelineInfo.renderPass(renderPass.renderPass);
                pipelineInfo.subpass(0);
                pipelineInfo.basePipelineHandle(VK10.VK_NULL_HANDLE);
                pipelineInfo.basePipelineIndex(-1);

                LongBuffer pGraphicsPipeline = stack.longs(VK10.VK_NULL_HANDLE);

                if (VK10.vkCreateGraphicsPipelines(this.device.device, VK10.VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create graphics pipeline!");
                }

                pipeline.pipeline = pGraphicsPipeline.get(0);
                pipeline.pipelineLayout = pPipelineLayout.get(0);

                shaders.destroyShader(shader);
            }
        }
    }
}
