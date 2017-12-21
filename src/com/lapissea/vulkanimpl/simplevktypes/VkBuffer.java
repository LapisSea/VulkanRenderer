package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.IMemoryAddressable;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryRequirements;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer extends ExtendableLong implements IMemoryAddressable{
	
	private long alignment=-1;
	private long size;
	
	public VkBuffer(long val, long size){
		super(val);
		this.size=size;
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyBuffer(device, get(), null);
		alignment=-1;
		val=0;
	}
	
	public long getAlignment(){
		return alignment;
	}
	
	@Override
	public VkDeviceMemory alocateMem(VkGpu gpu, int properties){
		
		VkDeviceMemory mem;
		try(MemoryStack stack=stackPush()){
			mem=gpu.alocateMem(size, getMemRequirements(gpu, stack), properties);
		}
		
		bind(gpu.getDevice(), mem);
		return mem;
	}
	
	@Override
	public VkMemoryRequirements getMemRequirements(VkGpu gpu, VkMemoryRequirements dest){
		vkGetBufferMemoryRequirements(gpu.getDevice(), get(), dest);
		return dest;
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory){
		bind(device, memory, 0);
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory, int offset){
		Vk.bindBufferMemory(device, this, memory, offset);
	}
}
