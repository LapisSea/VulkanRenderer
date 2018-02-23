package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VK10;

public class VkFence implements VkGpuCtx, VkDestroyable{
	
	public enum Status{SUCCESS, NOT_READY}
	
	private final VkGpu gpu;
	private final long  handle;
	
	public VkFence(VkGpuCtx gpuCtx, long handle){
		gpu=gpuCtx.getGpu();
		this.handle=handle;
	}
	
	public void waitFor(){
		Vk.waitForFence(this, handle);
	}
	
	public Status status(){
		return Vk.getFenceStatus(this, handle);
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyFence(getDevice(), handle, null);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle;
	}
}
