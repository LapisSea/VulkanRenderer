package com.lapissea.vulkannutcrack.simplevktypes;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkRenderPass extends ExtendableLong{
	
	public VkRenderPass(long val){
		super(val);
	}
	
	public void destroy(VkDevice device){
		vkDestroyRenderPass(device, get(), null);
		val=0;
	}
}
