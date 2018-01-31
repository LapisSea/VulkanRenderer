package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import static org.lwjgl.vulkan.KHRSurface.*;

public class VkSurface{
	
	public final long handle;
	
	private VkSurfaceFormatKHR format;
	private IVec2iR            size;
	
	public VkSurface(long handle){
		this.handle=handle;
	}
	
	public IVec2iR getSize(){
		return size;
	}
	
	public VkSurfaceCapabilitiesKHR getCapabilities(VkGpu gpu, MemoryStack stack){
		return getCapabilities(gpu, VkSurfaceCapabilitiesKHR.mallocStack(stack));
	}
	
	public VkSurfaceCapabilitiesKHR getCapabilities(VkGpu gpu, VkSurfaceCapabilitiesKHR capabilities){
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu.getPhysicalDevice(), handle, capabilities);
		return capabilities;
	}
}
