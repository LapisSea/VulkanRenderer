package com.lapissea.vulkanimpl.util;

import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

public interface VkGpuCtx{
	VkGpu getGpu();
	
	default VkDevice getGpuDevice(){
		return getGpu().getDevice();
	}
}
