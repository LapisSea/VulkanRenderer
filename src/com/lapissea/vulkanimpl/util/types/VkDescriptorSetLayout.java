package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VK10;

public class VkDescriptorSetLayout implements VkDestroyable, VkGpuCtx{
	
	private final long  handle;
	private final VkGpu gpu;
	
	public VkDescriptorSetLayout(long handle, VkGpu gpu){
		this.handle=handle;
		this.gpu=gpu;
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyDescriptorSetLayout(getDevice(), handle, null);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle;
	}
}
