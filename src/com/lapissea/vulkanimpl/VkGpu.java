package com.lapissea.vulkanimpl;

import com.lapissea.glfwwin.GlfwWindow;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkDestroyable, VkGpuCtx{
	
	private final GlfwWindow window;
	
	private      VkAllocationCallbacks allocator;
	public final VkPhysicalDevice      physicalDevice;
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	
	public VkGpu(GlfwWindow window, VkPhysicalDevice physicalDevice){
		this.window=window;
		this.physicalDevice=physicalDevice;
	}
	
	public VkAllocationCallbacks getAllocator(){
		return allocator;
	}
	
	@Override
	public void destroy(){
	
	}
	
	@Override
	public VkGpu getGpu(){
		return this;
	}
	
	
	public VkPhysicalDeviceMemoryProperties getMemoryProperties(){
		if(memoryProperties==null){
			memoryProperties=VkPhysicalDeviceMemoryProperties.calloc();
			vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		}
		
		return memoryProperties;
	}
	
	public int findMemoryType(int typeBits, int properties){
		VkPhysicalDeviceMemoryProperties deviceMemoryProperties=getMemoryProperties();
		
		int bits=typeBits;
		for(int i=0;i<VK_MAX_MEMORY_TYPES;i++){
			if((bits&1)==1){
				if((deviceMemoryProperties.memoryTypes(i).propertyFlags()&properties)==properties){
					return i;
				}
			}
			bits>>=1;
		}
		return -1;
	}
}
