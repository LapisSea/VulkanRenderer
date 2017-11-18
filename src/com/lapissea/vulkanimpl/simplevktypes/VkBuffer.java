package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryRequirements;

import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer extends ExtendableLong{
	
	private long alignment=-1;
	
	public VkBuffer(long val){
		super(val);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyBuffer(device, get(), null);
		alignment=-1;
		val=0;
	}
	
	public VkMemoryRequirements getMemRequirements(VkGpu gpu, MemoryStack stack){
		return getMemRequirements(gpu, VkMemoryRequirements.callocStack(stack));
	}
	
	public VkMemoryRequirements getMemRequirements(VkGpu gpu, VkMemoryRequirements dest){
		vkGetBufferMemoryRequirements(gpu.getDevice(), get(), dest);
		alignment=dest.alignment();
		return dest;
	}
	
	public long getAlignment(){
		return alignment;
	}
}
