package com.lapissea.vulkanimpl.util;

import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

public interface VkGpuCtx{
	
	VkGpu getGpu();
	
	default VkDevice getDevice(){
		UtilL.uncheckedThrow(new RuntimeException());
		return getGpu().getDevice();
	}
}
