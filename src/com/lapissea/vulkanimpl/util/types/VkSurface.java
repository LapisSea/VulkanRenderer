package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.VulkanCore;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import static org.lwjgl.vulkan.KHRSurface.*;

public class VkSurface implements VkDestroyable{
	
	public final long handle;
	
	private final VulkanCore renderer;
	
	public VkSurface(long handle, VulkanCore renderer){
		this.handle=handle;
		this.renderer=renderer;
	}
	
	
	public IVec2iR getSize(){
		return renderer.getWindow().size;
	}
	
	public VkSurfaceCapabilitiesKHR getCapabilities(VkGpu gpu, MemoryStack stack){
		return getCapabilities(gpu, VkSurfaceCapabilitiesKHR.mallocStack(stack));
	}
	
	public VkSurfaceCapabilitiesKHR getCapabilities(VkGpu gpu, VkSurfaceCapabilitiesKHR capabilities){
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu.getPhysicalDevice(), handle, capabilities);
		return capabilities;
	}
	
	@Override
	public void destroy(){
		vkDestroySurfaceKHR(renderer.getInstance(), handle, null);
	}
}
