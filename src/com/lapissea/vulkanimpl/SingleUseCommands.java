package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.simplevktypes.VkFence;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.vulkan.VK10.*;

public class SingleUseCommands implements AutoCloseable{
	
	public  VkCommandBuffer commandBuffer;
	public  MemoryStack     stack;
	private VkGpu           gpu;
	
	public SingleUseCommands(MemoryStack stack, VkGpu gpu){
		this.stack=stack;
		this.gpu=gpu;
		
		VkCommandBufferAllocateInfo allocInfo=VkCommandBufferAllocateInfo.callocStack(stack);
		allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
		allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
		allocInfo.commandPool(gpu.getTransferPool().get());
		allocInfo.commandBufferCount(1);
		
		commandBuffer=Vk.allocateCommandBuffers(gpu.getDevice(), allocInfo, stack.mallocPointer(1))[0];
		
		vkBeginCommandBuffer(commandBuffer, VkCommandBufferBeginInfo.callocStack(stack)
		                                                            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
		                                                            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
	}
	
	@Override
	public void close(){
		
		vkEndCommandBuffer(commandBuffer);
		
		VkFence fence=gpu.createFence();
		Vk.queueSubmit(gpu.getTransferQueue(),
		               VkSubmitInfo.callocStack(stack)
		                           .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
		                           .pCommandBuffers(stack.pointers(commandBuffer)),
		               fence);
		
		Vk.queueWaitIdle(gpu.getTransferQueue());
		fence.waitFor(gpu);
		
		vkFreeCommandBuffers(gpu.getDevice(), gpu.getTransferPool().get(), commandBuffer);
		fence.destroy();
		
		commandBuffer=null;
		stack=null;
		gpu=null;
	}
}
