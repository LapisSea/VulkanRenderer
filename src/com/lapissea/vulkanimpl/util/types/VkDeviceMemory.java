package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkBufferedLong;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceMemory extends VkBufferedLong implements VkGpuCtx{
	
	private final VkGpu gpu;
	
	public VkDeviceMemory(LongBuffer buff, VkGpu gpu){
		super(buff);
		this.gpu=gpu;
	}
	
	public static VkDeviceMemory alloc(VkGpuCtx ctx, VkMemoryAllocateInfo info){
		VkGpu      gpu=ctx.getGpu();
		LongBuffer lb =memAllocLong(1);
		
		int code=vkAllocateMemory(gpu.getDevice(), info, null, lb);
		if(DevelopmentInfo.DEV_ON) Vk.check(code);
		
		return new VkDeviceMemory(lb, gpu);
	}
	
	@Override
	public void destroy(){
		vkFreeMemory(gpu.getDevice(), buff.get(0), null);
		super.destroy();
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
