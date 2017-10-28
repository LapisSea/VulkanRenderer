package com.lapissea.vulkannutcrack.simplevktypes;

import com.lapissea.vulkannutcrack.Vk;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceMemory extends ExtendableLong{
	
	public VkDeviceMemory(long val){
		super(val);
	}
	
	public void destroy(VkDevice device){
		vkFreeMemory(device, get(), null);
		val=0;
	}
	
	public void bind(VkDevice device, VkBuffer buffer){
		bind(device, buffer, 0);
	}
	
	public void bind(VkDevice device, VkBuffer buffer, int offset){
		Vk.bindBufferMemory(device, buffer, this, offset);
	}
}
