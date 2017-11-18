package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkDescriptorPool extends ExtendableLong{
	
	public VkDescriptorPool(long val){
		super(val);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyDescriptorPool(device, get(), null);
		val=0;
	}
}
