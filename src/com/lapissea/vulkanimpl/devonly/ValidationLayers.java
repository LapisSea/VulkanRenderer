package com.lapissea.vulkanimpl.devonly;

import com.lapissea.vulkanimpl.Vk;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkLayerProperties;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.lapissea.vulkanimpl.VulkanRenderer.*;
import static org.lwjgl.system.MemoryStack.*;

public class ValidationLayers{
	
	static{ if(!DEVELOPMENT) throw new RuntimeException(); }
	
	public static void addLayers(Collection<String> layers){
		
		layers.add("VK_LAYER_LUNARG_standard_validation");
		
	}
	
	
	public static void initLogging(){
	
	}
	
}
