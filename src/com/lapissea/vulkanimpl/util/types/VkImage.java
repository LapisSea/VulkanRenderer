package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VkImageCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkImage implements VkDestroyable, VkGpuCtx{
	
	public static VkImage create(VkGpu gpu, VkImageCreateInfo info){
		LongBuffer handle=memAllocLong(1);
		try{
			int code=vkCreateImage(gpu.getDevice(), info, null, handle);
			if(DevelopmentInfo.DEV_ON) Vk.check(code);
			return new VkImage(handle, gpu);
		}catch(Throwable t){
			memFree(handle);
			throw t;
		}
	}
	
	private       LongBuffer handle;
	private final VkGpu      gpu;
	
	public VkImage(LongBuffer handle, VkGpu gpu){
		this.handle=handle;
		this.gpu=gpu;
	}
	
	@Override
	public void destroy(){
		vkDestroyImage(gpu.getDevice(), getHandle(), null);
		memFree(getBuff());
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public LongBuffer getBuff(){
		return handle;
	}
	
	public long getHandle(){
		return getBuff().get(0);
	}
}
