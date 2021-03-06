package com.lapissea.vulkanimpl.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.*;
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
	
	public static VkMemoryAllocateInfo memoryAllocateInfo(){
		return VkMemoryAllocateInfo.calloc().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
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
	
	public static VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo(){
		return VkDescriptorSetLayoutCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
	}
	
	public static VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo(MemoryStack stack){
		return VkDescriptorSetLayoutCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
	}
	
	public static VkDescriptorPoolCreateInfo descriptorPoolCreateInfo(){
		return VkDescriptorPoolCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
	}
	public static VkDescriptorPoolCreateInfo descriptorPoolCreateInfo(MemoryStack stack){
		return VkDescriptorPoolCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
	}
	
	public static VkDescriptorSetAllocateInfo descriptorSetAllocateInfo(){
		return VkDescriptorSetAllocateInfo.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
	}
	
	public static VkWriteDescriptorSet.Buffer writeDescriptorSet(int count, MemoryStack stack){
		VkWriteDescriptorSet.Buffer buff=VkWriteDescriptorSet.callocStack(count, stack);
		for(int i=0;i<count;i++){
			buff.get(i).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
		}
		return buff;
	}
	
	public static VkPresentInfoKHR presentInfoKHR(){
		return VkPresentInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
	}
	
	public static VkImageCreateInfo imageCreateInfo(){
		return VkImageCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
	}
	
	public static VkImageMemoryBarrier.Buffer imageMemoryBarrier(MemoryStack stack, int count){
		VkImageMemoryBarrier.Buffer buff=VkImageMemoryBarrier.callocStack(count, stack);
		for(int i=0;i<count;i++){
			buff.get(i).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
		}
		return buff;
	}
	
	public static VkSamplerCreateInfo samplerCreateInfo(){
		return VkSamplerCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
	}
	
	public static VkImageViewCreateInfo imageViewCreateInfo(){
		return VkImageViewCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
	}
	
	public static VkPipelineDepthStencilStateCreateInfo pipelineDepthStencilStateCreateInfo(MemoryStack stack){
		return VkPipelineDepthStencilStateCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
	}
	
	public static VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo(MemoryStack stack, LongBuffer setLayouts, VkPushConstantRange.Buffer pushConstantRanges){
		return VkPipelineLayoutCreateInfo.mallocStack(stack)
		                                 .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
		                                 .pNext(0)
		                                 .flags(0)
		                                 .pSetLayouts(setLayouts)
		                                 .pPushConstantRanges(pushConstantRanges);
	}
}
