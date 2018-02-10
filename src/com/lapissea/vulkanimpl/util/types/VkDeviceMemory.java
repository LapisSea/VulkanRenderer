package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceMemory implements VkGpuCtx, VkDestroyable{
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	
	public VkDeviceMemory(VkGpuCtx gpuCtx, LongBuffer dest){
		gpu=gpuCtx.getGpu();
		handle=dest;
	}
	
	@Override
	public void destroy(){
		vkFreeMemory(getDevice(), handle.get(0), null);
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public void bindBuffer(VkBuffer modelBuffer){
		vkBindBufferMemory(getDevice(), modelBuffer.getHandle(), handle.get(0), 0);
	}
	
	public void map(long offset,long size){
		
		vkMapMemory(getDevice(),handle.get(0),offset,size,);
		
	}
}
