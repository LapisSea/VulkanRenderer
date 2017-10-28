#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = color;
    outColor.r=pow(outColor.r,2);
    outColor.g=pow(outColor.g,2);
    outColor.b=pow(outColor.b,2);
}