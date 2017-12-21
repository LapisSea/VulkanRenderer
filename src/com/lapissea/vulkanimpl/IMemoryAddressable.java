package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.simplevktypes.VkDeviceMemory;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryRequirements;

public interface IMemoryAddressable{
	
	VkDeviceMemory alocateMem(VkGpu gpu, int properties);
	
	
	default VkMemoryRequirements getMemRequirements(VkGpu gpu, MemoryStack stack){
		return getMemRequirements(gpu, VkMemoryRequirements.callocStack(stack));
	}
	
	VkMemoryRequirements getMemRequirements(VkGpu gpu, VkMemoryRequirements dest);
}
