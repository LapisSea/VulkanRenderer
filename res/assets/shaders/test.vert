#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) out vec4 out_color;
layout(location = 1) out vec2 out_uv;
layout(location = 2) out float out_bulge;

out gl_PerVertex {
    vec4 gl_Position;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;


layout(binding = 0) uniform UniformBufferObject{
	mat4 model;
	mat4 view;
	mat4 proj;
	float mul;
} ubo;

void main() {
    gl_Position= ubo.proj * ubo.view * ubo.model * vec4(pos, 1);
    out_color = color;
    out_uv=uv;
    out_bulge=ubo.mul;
}