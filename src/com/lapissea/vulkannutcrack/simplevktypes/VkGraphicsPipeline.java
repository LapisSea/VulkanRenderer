package com.lapissea.vulkannutcrack.simplevktypes;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkGraphicsPipeline extends ExtendableLong{
	
	public VkGraphicsPipeline(long val){
		super(val);
	}
	
	public void destroy(VkDevice device){
		vkDestroyPipeline(device, get(), null);
		val=0;
	}
}
