package com.lapissea.vulkankimpl.simplevktypes;


import com.lapissea.vulkankimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkDescriptorSetLayout extends ExtendableLong{
	
	public VkDescriptorSetLayout(long val){
		super(val);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyDescriptorSetLayout(device, this.get(), null);
		val=0;
	}
}