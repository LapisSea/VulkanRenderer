#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) out vec4 out_color;
layout(location = 2) out vec2 out_pos;

out gl_PerVertex {
    vec4 gl_Position;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 color;


layout(binding = 0) uniform UniformBufferObject{
	mat4 model;
	mat4 view;
	mat4 projection;
	float mul;
} ubo;

void main() {
	vec4 lel=ubo.projection*ubo.view*ubo.model*vec4(pos, 1);
    gl_Position= vec4(lel.xyz, 1);//+vec4(pos, 1.0)*(1-ubo.mul);
    out_color = color;
    out_pos=pos.xy;
}