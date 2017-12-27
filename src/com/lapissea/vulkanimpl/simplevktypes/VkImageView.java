package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	private final VkGpu gpu;
	
	public VkImageView(VkGpuCtx gpuCtx, long val){
		super(val);
		gpu=gpuCtx.getGpu();
		if(Vk.DEBUG) Objects.requireNonNull(gpu);
	}
	
	@Override
	public void destroy(){
		vkDestroyImageView(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
