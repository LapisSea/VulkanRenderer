package com.lapissea.vulkanimpl.renderer.shader.nativetypes;

import java.util.ArrayList;
import java.util.List;

public class ShaderType{
	
	private static final List<ShaderType> TYPE_LIST=new ArrayList<>();
	
	static{
//		vec3
		TYPE_LIST.add(new ShaderType("vec3", float.class, float.class, float.class));
	}
	
	public final List<Class<?>> primitives;
	
	public ShaderType(String nativeName, Class<?>... classes){
		primitives=List.of(classes);
	}
}
