package com.lapissea.vulkanimpl;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

public enum VkPhysicalDeviceType{
	OTHER(VK10.VK_PHYSICAL_DEVICE_TYPE_OTHER, 0),
	INTEGRATED_GPU(VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU, 1),
	DISCRETE_GPU(VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU, 2),
	VIRTUAL_GPU(VK10.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU, 2),
	CPU(VK10.VK_PHYSICAL_DEVICE_TYPE_CPU, 0);
	
	public final int id, rating;
	
	VkPhysicalDeviceType(int id, int value){
		this.id=id;
		this.rating=value;
	}
	
	public static VkPhysicalDeviceType find(int id){
		for(VkPhysicalDeviceType t : VkPhysicalDeviceType.values()){
			if(t.id==id) return t;
		}
		return OTHER;
	}
	
	public static VkPhysicalDeviceType find(VkPhysicalDeviceProperties properties){
		return find(properties.deviceType());
	}
}
