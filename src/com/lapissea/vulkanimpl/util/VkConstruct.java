package com.lapissea.vulkanimpl.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.DEVMODE;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.io.File;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkConstruct{
	
	public static VkInstanceCreateInfo instanceCreateInfo(MemoryStack stack){
		return VkInstanceCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
	}
	
	public static VkBufferCreateInfo bufferCreateInfo(MemoryStack stack){
		return VkBufferCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
	}
	public static VkBufferCreateInfo bufferCreateInfo(){
		return VkBufferCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
	}
	
	public static VkMemoryAllocateInfo memoryAllocateInfo(MemoryStack stack){
		return VkMemoryAllocateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
	}
	
}
