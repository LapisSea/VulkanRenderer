package com.lapissea.vulkanimpl.devonly;

import com.lapissea.vulkanimpl.util.DevelopmentInfo;

import java.util.Collection;

public class ValidationLayers{
	
	static{ DevelopmentInfo.checkOnLoad(); }
	
	public static void addLayers(Collection<String> layers){
		
		layers.add("VK_LAYER_LUNARG_standard_validation");
		
	}
	
	
}
