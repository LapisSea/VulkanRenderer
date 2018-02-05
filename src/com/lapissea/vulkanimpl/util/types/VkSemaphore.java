package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.nio.LongBuffer;

public class VkSemaphore implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	
	public VkSemaphore(VkGpu gpu, LongBuffer handle){
		this.gpu=gpu;
		this.handle=handle;
	}
	
	@Override
	public void destroy(){
	
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
