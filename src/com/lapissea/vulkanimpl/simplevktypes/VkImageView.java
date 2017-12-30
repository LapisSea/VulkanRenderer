package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VkImageView extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	private final VkGpuCtx gpuCtx;
	
	public VkImageView(VkGpuCtx gpuCtx, long val){
		super(val);
		this.gpuCtx=gpuCtx;
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpuCtx);
	}
	
	@Override
	public void destroy(){
		vkDestroyImageView(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpuCtx.getGpu();
	}
}
