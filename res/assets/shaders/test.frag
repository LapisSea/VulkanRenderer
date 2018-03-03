#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 uv;
layout(location = 2) in float bulge;

layout(location = 0) out vec4 fragmentColor;

layout(binding = 1) uniform sampler2D texSampler;

void main() {
    fragmentColor = texture(texSampler, uv);
}