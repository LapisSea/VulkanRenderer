package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkCommandPool implements VkDestroyable, VkGpuCtx{
	
	
	private final VkGpu gpu;
	private final long  handle;
	
	public VkCommandPool(VkGpu gpu, long handle){
		this.gpu=gpu;
		this.handle=handle;
	}
	
	public VkCommandBufferM allocateCommandBuffer(){
		return allocateCommandBuffer(1)[0];
	}
	
	public VkCommandBufferM[] allocateCommandBuffer(int count, VkCommandBufferM primary){
		if(!primary.isPrimary) throw new RuntimeException("Can't make secondary buffer from a secondary buffer!");
		return allocateCommandBuffer(count, VK_COMMAND_BUFFER_LEVEL_SECONDARY);
	}
	
	public VkCommandBufferM[] allocateCommandBuffer(int count){
		return allocateCommandBuffer(count, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
	}
	
	private VkCommandBufferM[] allocateCommandBuffer(int count, int level){
		try(MemoryStack stack=stackPush()){
			VkCommandBufferAllocateInfo allocInfo=VkCommandBufferAllocateInfo.callocStack(stack);
			allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
			         .commandPool(handle)
			         .level(level)
			         .commandBufferCount(count);
			return Vk.allocateCommandBuffers(getGpu(), this, allocInfo, stack.mallocPointer(count));
		}
	}
	
	@Override
	public void destroy(){
		vkDestroyCommandPool(getDevice(), handle, null);
		
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle;
	}
}