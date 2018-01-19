package com.lapissea.vulkanimpl.memory;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;

import static org.lwjgl.vulkan.VK10.*;

public class Block{
	final VkDeviceMemory mem;
	
	final long offset;
	final long size;
	
	boolean free=true;
	
	public Block(VkDeviceMemory mem, long offset, long size){
		this.mem=mem;
		this.offset=offset;
		this.size=size;
	}
	
	public int findMemoryType(VkGpu gpu, int memBits, boolean local){
		if(local) return gpu.findMemoryType(memBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		int required=VK_MEMORY_PROPERTY_HOST_COHERENT_BIT|VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
			optional=required|VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
		
		int optionalType=gpu.findMemoryType(memBits, optional);
		if(optionalType==-1) return gpu.findMemoryType(memBits, required);
		return optionalType;
	}
	
}
