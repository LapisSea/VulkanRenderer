package com.lapissea.vulkannutcrack.simplevktypes;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkSemaphore extends ExtendableLong{
	
	public VkSemaphore(long val){
		super(val);
	}
	
	public void destroy(VkDevice device){
		vkDestroySemaphore(device, get(), null);
		val=0;
	}
	
}