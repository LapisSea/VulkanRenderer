package com.lapissea.vulkankimpl.simplevktypes;

import com.lapissea.vulkankimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSemaphore extends ExtendableLong{
	
	private final LongBuffer buff;
	
	public VkSemaphore(LongBuffer buff){
		super(buff.get(0));
		this.buff=buff;
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroySemaphore(device, get(), null);
		memFree(buff);
		val=0;
	}
	
	public LongBuffer buff(){
		return buff;
	}
}