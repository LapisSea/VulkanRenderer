#version 450
#extension GL_ARB_separate_shader_objects : enable

out gl_PerVertex {
	vec4 gl_Position;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec2 uv;

layout(location = 0) out vec3 out_toCameraVector;
layout(location = 1) out vec2 out_uv;
layout(location = 2) out vec3 out_camPos;
layout(location = 3) out vec3 out_toPointLight;

layout(binding = 0) uniform UniformBufferObject{
	mat4 model;
	mat4 view;
	mat4 proj;
} ubo;

void main() {
	out_camPos=(inverse(ubo.view)*vec4(0,0,0,1)).xyz;

	vec3 norm=(vec4(normal,0)*ubo.model).xyz;
	vec3 tang=(vec4(tangent,0)*ubo.model).xyz;
	vec3 biTang=normalize(cross(norm, tang));

	mat3 toTangentSpace=mat3(
		tang.x, biTang.x, norm.x,
		tang.y, biTang.y, norm.y,
		tang.z, biTang.z, norm.z
	);


	vec4 worldSpace=vec4(pos, 1)*ubo.model;
	vec4 camSpace=ubo.view * worldSpace;
	vec4 tangSpace=ubo.view * worldSpace;

	gl_Position=ubo.proj * camSpace;
	vec3 c=(-worldSpace.xyz)+out_camPos;
	out_toCameraVector=toTangentSpace*c;
	out_uv=uv;
}