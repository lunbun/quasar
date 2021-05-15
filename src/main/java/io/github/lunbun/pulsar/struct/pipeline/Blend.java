package io.github.lunbun.pulsar.struct.pipeline;

import org.lwjgl.vulkan.VK10;

/*
https://vulkan-tutorial.com/Drawing_a_triangle/Graphics_pipeline_basics/Fixed_functions#page_Color-blending
Blend pseudo:

if (blendEnable) {
    finalColor.rgb = (srcColorBlendFactor * newColor.rgb) <colorBlendOp> (dstColorBlendFactor * oldColor.rgb);
    finalColor.a = (srcAlphaBlendFactor * newColor.a) <alphaBlendOp> (dstAlphaBlendFactor * oldColor.a);
} else {
    finalColor = newColor;
}

finalColor = finalColor & colorWriteMask;
 */
public final class Blend {
    public final Factor srcColorBlendFactor;
    public final Operator colorBlendOp;
    public final Factor dstColorBlendFactor;
    public final Factor srcAlphaBlendFactor;
    public final Operator alphaBlendOp;
    public final Factor dstAlphaBlendFactor;

    public Blend(Factor srcColorBlendFactor, Operator colorBlendOp, Factor dstColorBlendFactor,
                 Factor srcAlphaBlendFactor, Operator alphaBlendOp, Factor dstAlphaBlendFactor) {
        this.srcColorBlendFactor = srcColorBlendFactor;
        this.colorBlendOp = colorBlendOp;
        this.dstColorBlendFactor = dstColorBlendFactor;
        this.srcAlphaBlendFactor = srcAlphaBlendFactor;
        this.alphaBlendOp = alphaBlendOp;
        this.dstAlphaBlendFactor = dstAlphaBlendFactor;
    }

    public enum Factor {
        SRC_ALPHA(VK10.VK_BLEND_FACTOR_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA),
        ZERO(VK10.VK_BLEND_FACTOR_ZERO),
        ONE(VK10.VK_BLEND_FACTOR_ONE);

        public final int vk;

        Factor(int vk) {
            this.vk = vk;
        }
    }

    public enum Operator {
        ADD(VK10.VK_BLEND_OP_ADD),
        SUBTRACT(VK10.VK_BLEND_OP_SUBTRACT),
        MIN(VK10.VK_BLEND_OP_MIN),
        MAX(VK10.VK_BLEND_OP_MAX),
        REVERSE_SUBTRACT(VK10.VK_BLEND_OP_REVERSE_SUBTRACT);

        public final int vk;

        Operator(int vk) {
            this.vk = vk;
        }
    }
}
