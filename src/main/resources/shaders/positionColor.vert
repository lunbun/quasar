#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform UniformBufferObject {
    mat4 matrix;
} ubo;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in uint inColor;

layout(location = 0) out vec4 fragColor;

void main() {
    gl_Position = ubo.matrix * vec4(inPosition, 1.0);
    fragColor = vec4(((inColor >> 24u) & 255u) / 255., ((inColor >> 16u) & 255u) / 255.,
                    ((inColor >> 8u) & 255u) / 255., (inColor & 255u) / 255.);
}