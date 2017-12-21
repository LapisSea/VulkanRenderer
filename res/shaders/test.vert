#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform UniformBufferObject{
    mat4 model;
    mat4 view;
    mat4 proj;
    float tim;
} ubo;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 inColor;
layout(location = 2) in vec2 inUv;

layout(location = 0) out vec4 color;
layout(location = 1) out vec2 uv;
layout(location = 2) out float tim;

out gl_PerVertex {
    vec4 gl_Position;
};

void main() {
	vec4 p4=vec4(pos, 1.0);
    gl_Position=ubo.proj*ubo.view*ubo.model*p4;

	uv=inUv;
	tim=ubo.tim;
    color = inColor;
}