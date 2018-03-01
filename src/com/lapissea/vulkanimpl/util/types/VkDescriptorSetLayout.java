package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VK10;

import java.nio.LongBuffer;

public class VkDescriptorSetLayout implements VkDestroyable, VkGpuCtx{
	
	private final LongBuffer buffer;
	private final VkGpu      gpu;
	
	public VkDescriptorSetLayout(LongBuffer buffer, VkGpu gpu){
		this.buffer=buffer.asReadOnlyBuffer();
		this.gpu=gpu;
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyDescriptorSetLayout(getDevice(), getHandle(), null);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return buffer.get(0);
	}
	
	public LongBuffer getBuffer(){
		return buffer;
	}
}
