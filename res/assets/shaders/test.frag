#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 uv;
layout(location = 2) in float angle;

layout(location = 0) out vec4 fragmentColor;

void main() {
	float siz=500;
	float lel=sin(uv.x*siz*sin(angle))*cos(uv.y*siz*cos(angle));
    fragmentColor = color*lel;
}