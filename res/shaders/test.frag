#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 uv;
layout(location = 2) in float tim;

layout(location = 0) out vec4 outColor;

void main() {
//	if(length(uv)>0.5)discard;
	outColor = color*sin(length(uv)*(uv.x*600))*sin(uv.x*uv.y*600+tim);
//	outColor.r=pow(outColor.r,2);
//	outColor.g=pow(outColor.g,2);
//	outColor.b=pow(outColor.b,2);
}