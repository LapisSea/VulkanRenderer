package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;

import java.nio.LongBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSemaphore extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	private final LongBuffer buff;
	private final VkGpu      gpu;
	
	public VkSemaphore(VkGpu gpu, LongBuffer buff){
		super(buff.get(0));
		this.buff=buff;
		if(Vk.DEBUG) Objects.requireNonNull(gpu);
		this.gpu=gpu;
	}
	
	@Override
	public void destroy(){
		vkDestroySemaphore(getGpu().getDevice(), get(), null);
		memFree(buff);
		val=0;
	}
	
	public LongBuffer buff(){
		return buff;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}