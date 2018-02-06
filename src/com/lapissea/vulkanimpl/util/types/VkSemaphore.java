package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSemaphore implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	
	public VkSemaphore(VkGpu gpu, LongBuffer handle){
		this.gpu=gpu;
		this.handle=handle;
	}
	
	@Override
	public void destroy(){
		vkDestroySemaphore(getDevice(), handle.get(0), null);
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public LongBuffer getBuffer(){
		return handle;
	}
}
