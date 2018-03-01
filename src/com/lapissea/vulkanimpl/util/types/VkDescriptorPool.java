package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDescriptorPool implements VkDestroyable, VkGpuCtx{
	private final VkGpu gpu;
	private final long  handle;
	
	public VkDescriptorPool(VkGpu gpu, long handle){
		this.gpu=gpu;
		this.handle=handle;
	}
	
	public long allocateDescriptorSets(LongBuffer layouts){
		LongBuffer dest=memAllocLong(1);
		try(VkDescriptorSetAllocateInfo allocInfo=VkConstruct.descriptorSetAllocateInfo()){
			allocInfo.descriptorPool(handle)
			         .pSetLayouts(layouts);
			
			return Vk.allocateDescriptorSets(gpu, allocInfo, dest);
		}finally{
			memFree(dest);
		}
	}
	
	public long getHandle(){
		return handle;
	}
	
	@Override
	public void destroy(){
		vkDestroyDescriptorPool(getDevice(), handle, null);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
