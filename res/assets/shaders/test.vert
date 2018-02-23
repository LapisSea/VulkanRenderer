#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) out vec4 out_color;
layout(location = 1) out vec2 out_uv;
layout(location = 2) out float angle;

out gl_PerVertex {
    vec4 gl_Position;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;


void main() {
    gl_Position = vec4(pos, 1.0);
    out_color = color;
    out_uv=uv;
    angle=atan((pos.x+0.5),-(pos.y+0.5));
}