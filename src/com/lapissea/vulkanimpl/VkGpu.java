package com.lapissea.vulkanimpl;

import com.lapissea.glfwwin.GlfwWindow;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkDestroyable, VkGpuCtx{
	
	private final GlfwWindow window;
	
	private VkAllocationCallbacks allocator;
	
	private final VkPhysicalDevice physicalDevice;
	private       VkDevice         logicalDevice;
	
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	private VkPhysicalDeviceFeatures         features;
	
	
	public VkGpu(GlfwWindow window, VkPhysicalDevice physicalDevice){
		this.window=window;
		this.physicalDevice=physicalDevice;
		
		memoryProperties=VkPhysicalDeviceMemoryProperties.malloc();
		vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		
		features=VkPhysicalDeviceFeatures.malloc();
		vkGetPhysicalDeviceFeatures(physicalDevice, features);
		
	}
	
	public boolean init(){
		if(logicalDevice!=null) return false;
		
		try(MemoryStack stack=stackPush()){
			VkDeviceCreateInfo info=VkDeviceCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
			logicalDevice=Vk.createDevice(physicalDevice, info, stack.mallocPointer(1));
		}
		
		return true;
	}
	
	@Override
	public void destroy(){
		
		if(logicalDevice!=null){
			vkDestroyDevice(logicalDevice, allocator);
			
		}
		
		memoryProperties=null;
		features=null;
		logicalDevice=null;
	}
	
	@Override
	public VkGpu getGpu(){
		return this;
	}
	
	
	public int findMemoryType(int typeBits, int properties){
		
		int bits=typeBits;
		for(int i=0;i<VK_MAX_MEMORY_TYPES;i++){
			if((bits&1)==1){
				if((memoryProperties.memoryTypes(i).propertyFlags()&properties)==properties){
					return i;
				}
			}
			bits>>=1;
		}
		return -1;
	}
	
	public VkPhysicalDeviceFeatures getFeatures(){
		return features;
	}
	
	public VkAllocationCallbacks getAllocator(){
		return allocator;
	}
	
	public VkPhysicalDeviceMemoryProperties getMemoryProperties(){
		return memoryProperties;
	}
}
