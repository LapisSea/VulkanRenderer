package com.lapissea.vulkanimpl.renderer.shader.nativetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShaderType{
	
	private static final List<ShaderType> TYPE_LIST=new ArrayList<>();
	
	public static class StVec3 extends ShaderType{
		
		public StVec3(){
			super("vec3", float.class, float.class, float.class);
		}
		
	}
	
	public static final StVec3 VEC3=new StVec3();
	
	static{
//		vec3
		TYPE_LIST.add(new ShaderType("vec3", float.class, float.class, float.class));
	}
	
	public final List<Class<?>> primitives;
	
	public ShaderType(String nativeName, Class<?>... classes){
		primitives=Arrays.asList(classes);
	}
}
