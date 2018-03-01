#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 uv;
layout(location = 2) in float bulge;

layout(location = 0) out vec4 fragmentColor;

void main() {
	float siz=1000;
	float angle=atan(-uv.y,uv.x);
	if(angle<0)angle=3.14*2+angle;
	float length=pow(length(uv),1+bulge)/2;
	float lel=sin(length*siz*sin(angle)+3.14/2)*cos(length*siz*cos(angle));
    fragmentColor = color*max(lel,0);
}