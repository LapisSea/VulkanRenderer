package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VkPipelineLayout extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu gpu;
	
	public VkPipelineLayout(VkGpuCtx gpuCtx, long val){
		super(val);
		gpu=gpuCtx.getGpu();
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpu);
	}
	
	@Override
	public void destroy(){
		vkDestroyPipelineLayout(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
