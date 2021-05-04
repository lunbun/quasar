package io.github.lunbun.pulsar.struct.pipeline;

import io.github.lunbun.pulsar.component.pipeline.RenderPass;

public final class GraphicsPipeline {
    public long pipelineLayout;
    public long pipeline;
    public final RenderPass renderPass;

    public GraphicsPipeline(RenderPass renderPass) {
        this.renderPass = renderPass;
    }
}
