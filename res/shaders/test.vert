#version 450
#extension GL_ARB_separate_shader_objects : enable

out gl_PerVertex {
    vec4 gl_Position;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 inColor;

layout(location = 0) out vec4 color;

void main() {
    gl_Position = vec4(pos, 1.0);

    color = inColor;
}