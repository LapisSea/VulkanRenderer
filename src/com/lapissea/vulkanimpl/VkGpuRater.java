package com.lapissea.vulkanimpl;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import java.util.Collection;
import java.util.stream.Collectors;

public class VkGpuRater{
	
	private static boolean checkDeviceExtensions(MemoryStack stack, VkPhysicalDevice device, Collection<? extends CharSequence> deviceExtensions){
		return Vk.getDeviceExtensionProperties(stack, device, stack.mallocInt(1))
		         .stream()
		         .map(VkExtensionProperties::extensionNameString)
		         .collect(Collectors.toList())
		         .containsAll(deviceExtensions);
	}
	
	public static int rateGpuInit(VkGpu gpu, MemoryStack stack){
		VkPhysicalDeviceFeatures features=gpu.getFeatures();
		if(!features.geometryShader()||
		   !features.shaderClipDistance()||
		   !features.shaderTessellationAndGeometryPointSize()||
		   !features.tessellationShader()
			) return -1;
		
		if(!checkDeviceExtensions(stack, gpu.physicalDevice, gpu.deviceExtensions)) return -1;
		
		if(gpu.getQueueGraphicsId()==-1||
		   gpu.getQueueSurfaceId()==-1||
		   gpu.getFormats().capacity()==0||
		   gpu.getPresentModes().capacity()==0
			) return -1;
		
		int rating=0;
		rating+=VkPhysicalDeviceType.find(gpu.getProperties()).rating*100;
		
		return rating;
	}
	
	
}
