package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView extends ExtendableLong{
	
	public VkImageView(long val){
		super(val);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyImageView(device, get(), null);
		val=0;
	}
}
