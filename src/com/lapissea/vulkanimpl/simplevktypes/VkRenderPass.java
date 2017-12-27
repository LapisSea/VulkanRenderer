package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VkDevice;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VkRenderPass extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu gpu;
	
	public VkRenderPass(VkGpuCtx gpuCtx, long val){
		super(val);
		gpu=gpuCtx.getGpu();
		if(Vk.DEBUG) Objects.requireNonNull(gpu);
	}
	
	@Override
	public void destroy(){
		vkDestroyRenderPass(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
