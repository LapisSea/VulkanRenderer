package com.lapissea.vulkanimpl.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

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
	
	public static VkCommandBufferBeginInfo commandBufferBeginInfo(MemoryStack stack){
		return VkCommandBufferBeginInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
	}
	
	public static VkCommandBufferBeginInfo commandBufferBeginInfo(){
		return VkCommandBufferBeginInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
	}
	
	public static VkCommandBufferAllocateInfo commandBufferAllocateInfo(MemoryStack stack){
		return VkCommandBufferAllocateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
	}
	
	public static VkSubmitInfo submitInfo(MemoryStack stack){
		return VkSubmitInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
	}
	
	public static VkSubmitInfo submitInfo(){
		return VkSubmitInfo.calloc().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
	}
	
	public static VkSemaphoreCreateInfo semaphoreCreateInfo(){
		return VkSemaphoreCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
	}
	
	public static VkFenceCreateInfo fenceCreateInfo(){
		return VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
	}
	
}