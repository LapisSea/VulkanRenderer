package com.lapissea.vulkanimpl.simplevktypes;

public class VkDescriptorSet extends ExtendableLong{
	
	public VkDescriptorSet(long val){
		super(val);
	}
	
//	public void destroy(VkGpu gpu){
//		destroy(gpu.getDevice());
//	}
//
//	public void destroy(VkDevice device){
//		vkDestroyFramebuffer(device, get(), null);
//		val=0;
//	}
}
