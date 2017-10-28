package com.lapissea.vulkannutcrack.simplevktypes;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkPipelineLayout extends ExtendableLong{
	
	public VkPipelineLayout(long val){
		super(val);
	}
	
	public void destroy(VkDevice device){
		vkDestroyPipelineLayout(device, get(), null);
		val=0;
	}
}
