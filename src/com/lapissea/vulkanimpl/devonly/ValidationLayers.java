package com.lapissea.vulkanimpl.devonly;

import java.util.Collection;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;

public class ValidationLayers{
	
	static{ if(!DEVELOPMENT) throw new RuntimeException(); }
	
	public static void addLayers(Collection<String> layers){
		
		layers.add("VK_LAYER_LUNARG_standard_validation");
		
	}
	
	
	public static void initLogging(){
	
	}
	
}
