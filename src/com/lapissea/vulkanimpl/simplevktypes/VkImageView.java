package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;

public class VkImageView extends ExtendableLong{
	
	public VkImageView(long val){
		super(val);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		VK10.vkDestroyImageView(device, get(), null);
		val=0;
	}
}
