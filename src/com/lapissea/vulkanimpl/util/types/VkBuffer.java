package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class VkBuffer implements VkGpuCtx, VkDestroyable{
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	public final  long       size;
	
	public VkBuffer(VkGpuCtx gpuCtx, LongBuffer dest, long size){
		gpu=gpuCtx.getGpu();
		handle=dest;
		this.size=size;
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyBuffer(getDevice(), handle.get(0), null);
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public LongBuffer getBuff(){
		return handle;
	}
	
	public VkDeviceMemory allocateBufferMemory(int requestedProperties){
		try(VkMemoryRequirements memRequirements=VkMemoryRequirements.malloc()){
			VkDeviceMemory mem=getGpu().allocateMemory(gpu.getMemRequirements(memRequirements, this), size, requestedProperties);
			mem.bindBuffer(this);
			return mem;
		}
	}
}
