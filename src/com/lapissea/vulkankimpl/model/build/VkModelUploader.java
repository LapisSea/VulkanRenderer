package com.lapissea.vulkankimpl.model.build;

import com.lapissea.vulkankimpl.BufferUtil;
import com.lapissea.vulkankimpl.Vk;
import com.lapissea.vulkankimpl.VkGpu;
import com.lapissea.vulkankimpl.model.VkBufferMemory;
import com.lapissea.vulkankimpl.model.VkModel;
import com.lapissea.vulkankimpl.simplevktypes.VkBuffer;
import com.lapissea.vulkankimpl.simplevktypes.VkFence;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static com.lapissea.vulkankimpl.BufferUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelUploader{
	
	public static VkModel upload(VkGpu gpu, VkModelBuilder modelBuilder){
		try(MemoryStack stack=MemoryStack.stackPush()){
			int size=modelBuilder.size(),
				dataSize=modelBuilder.dataSize(),
				indexCount=modelBuilder.getIndexCount();
			boolean indices16Bit=modelBuilder.indices16Bit();
			
			VkBufferMemory stagingMemory=createBufferMem(gpu, size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			stagingMemory.requestMemory(gpu.getDevice(), stack, buff->{
				modelBuilder.exportData(buff);
				modelBuilder.exportIndices(buff);
			});
			
			VkBufferMemory memory=createBufferMem(gpu, size, VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			
			copyBuffer(gpu, stagingMemory.getBuffer(), memory.getBuffer(), size);
			memory.flushMemory(gpu);
			stagingMemory.destroy(gpu.getDevice());
			gpu.waitIdle();
			
			return new VkModel(memory, modelBuilder.format, dataSize, indices16Bit?VK_INDEX_TYPE_UINT16:VK_INDEX_TYPE_UINT32, indexCount!=0?indexCount:size/modelBuilder.format.getSizeBytes());
		}
	}
	
	private static void copyBuffer(VkGpu gpu, VkBuffer srcBuffer, VkBuffer dstBuffer, int size){
		try(MemoryStack stack=MemoryStack.stackPush()){
			VkCommandBufferAllocateInfo allocInfo=VkCommandBufferAllocateInfo.callocStack(stack);
			allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
			allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
			allocInfo.commandPool(gpu.getTransferPool().get());
			allocInfo.commandBufferCount(1);
			
			VkCommandBuffer commandBuffer=Vk.allocateCommandBuffers(gpu.getDevice(), allocInfo, stack.mallocPointer(1))[0];
			
			vkBeginCommandBuffer(commandBuffer, VkCommandBufferBeginInfo.callocStack(stack)
			                                                            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
			                                                            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
			
			vkCmdCopyBuffer(commandBuffer, srcBuffer.get(), dstBuffer.get(), VkBufferCopy.callocStack(1, stack).size(size));
			
			vkEndCommandBuffer(commandBuffer);
			
			VkFence fence=gpu.createFence();
			Vk.queueSubmit(gpu.getTransferQueue(), VkSubmitInfo.callocStack(stack)
			                                                   .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
			                                                   .pCommandBuffers(BufferUtil.buffSingle(stack, commandBuffer)),
			               fence.get());
			
			Vk.queueWaitIdle(gpu.getTransferQueue());
			fence.waitFor(gpu);

			vkFreeCommandBuffers(gpu.getDevice(), gpu.getTransferPool().get(), commandBuffer);
			fence.destroy(gpu);
		}
	}
	
	
}
