package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VkFramebuffer extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu gpu;
	
	public VkFramebuffer(VkGpu gpu, long val){
		super(val);
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpu);
		this.gpu=gpu;
	}
	
	
	@Override
	public void destroy(){
		vkDestroyFramebuffer(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
