#version 450
#extension GL_ARB_separate_shader_objects : enable

out gl_PerVertex {
    vec4 gl_Position;
};
/*
layout(location = 0) in vec2 inPosition;
*/
layout(location = 0) in vec3 inColor;

layout(location = 0) out vec4 color;

vec2 positions[3] = vec2[](
    vec2(0.5, 0.5),
    vec2(0.0, -0.5),
    vec2(-0.5, 0.5)
);

void main() {
    gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
    color = vec4(inColor, 1)+0.2;
}