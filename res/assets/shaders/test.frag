#version 450
#extension GL_ARB_separate_shader_objects : enable

struct DirLight{
	vec3 normal;
	vec3 color;
};

struct PointLight{
	vec3 color;
	vec3 attenuation;
	float rad;
};

struct Camera{
	float gamma;
	float exposure;
};

float calcAttenuation(PointLight pt, float distance){
	return 1.0/(
		pt.attenuation.x+
		pt.attenuation.y*distance+
		pt.attenuation.z*distance*distance
	);
}

layout(binding = 1) uniform UniformLighting{
	DirLight sun;
	PointLight pt;
} lights;

layout(location = 0) in vec3 toCameraVector;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec3 camPos;
layout(location = 3) in vec3 toPointLight;

layout(location = 0) out vec4 fragmentColor;

layout(binding = 2) uniform sampler2D diffuse;
layout(binding = 3) uniform sampler2D normal;


vec3 colorMap(vec3 colorIn){
    float gamma = 1;
    float exposure = 1;
	
    vec3 mapped = vec3(1.0) - exp(-colorIn * exposure);
    mapped = pow(mapped, vec3(1.0 / gamma));
	return mapped;
}

void main() {
    fragmentColor = texture(diffuse, uv);
	if(fragmentColor.a<1/256.0)discard;

	vec3 brightness=vec3(0.0); 
	vec3 specular=vec3(0.0);

	//tangent sapce normal solve
	vec3 bump=texture(normal, uv).rgb;
	vec3 normal;

	if(bump==vec3(0))normal = vec3(0,0,-1);//no bump map
	else normal=-normalize(bump*2 - 1); //map bump to normal
	
	if(gl_FrontFacing)normal*=-1;
	
	{
		DirLight sun=lights.sun;

		float normalDot=dot(normal, normalize(vec3(0,1,1)));

		// if(normalDot>0){
			brightness+=sun.color*normalDot/5;
		// }
	}

	{
		PointLight pt=lights.pt;
		vec3 toLight=toCameraVector;
		
		float dist=length(toLight);

		if(dist<=pt.rad){
			//if(dist<=pt.rad/30)discard;
			float attFact= calcAttenuation(pt,dist);

			float cutoff=2/256.0;
			if(attFact>=cutoff){
				
				attFact*=1+cutoff;
				attFact-=cutoff;
				
				vec3 unitToLight=normalize(toLight);
				float normalDot=dot(normal, -unitToLight);
				
				if(normalDot>0){
					brightness+=pt.color*attFact*normalDot;
					specular+=pt.color*attFact*pow(normalDot,100);
				}
			}//else specular+=abs(toLight)/10;
		}
		
		
	}

	fragmentColor.rgb*=brightness+0.1;
	fragmentColor.rgb+=specular;

	fragmentColor.rgb=colorMap(fragmentColor.rgb);
	
	// fragmentColor.rgb=vec3(uv,0);
	
	// fragmentColor.rgb+=0.2;
	// fragmentColor.rg=uv;
    // fragmentColor=vec4(bump, 1);
}
