#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 uv;
layout(location = 2) in float tim;

layout(location = 0) out vec4 outColor;

layout(binding = 1) uniform sampler2D texSampler;

void main() {
	outColor=texture(texSampler, uv);

//	if(outColor.b==max(outColor.r,max(outColor.g,outColor.b)))discard;
//	outColor.r=pow(outColor.r,2);
//	outColor.g=pow(outColor.g,2);
//	outColor.b=pow(outColor.b,2);
}