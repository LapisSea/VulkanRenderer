package com.lapissea.vulkannutcrack.model.build;

import com.lapissea.vulkannutcrack.BufferUtil;
import com.lapissea.vulkannutcrack.Vk;
import com.lapissea.vulkannutcrack.VkGpu;
import com.lapissea.vulkannutcrack.model.VkBufferMemory;
import com.lapissea.vulkannutcrack.model.VkModel;
import com.lapissea.vulkannutcrack.simplevktypes.VkBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VkModelUploader{
	
	public static VkModel upload(VkGpu gpu, VkModelBuilder modelBuilder){
		try(MemoryStack stack=MemoryStack.stackPush()){
			int size=modelBuilder.size(),
					dataSize=modelBuilder.dataSize(),
					indexCount=modelBuilder.getIndexCount();
			boolean indices16Bit=modelBuilder.indices16Bit();
			
			VkBufferMemory stagingMemory=createBuffer(gpu, stack, size,
			                                          VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
			                                          VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			stagingMemory.requestMemory(gpu.getDevice(), stack, buff->{
				modelBuilder.exportData(buff);
				modelBuilder.exportIndices(buff);
			});
			
			VkBufferMemory memory=createBuffer(gpu, stack, size,
			                                   VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
			                                   VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			
			copyBuffer(gpu, stagingMemory.getBuffer(), memory.getBuffer(), size);
			
			stagingMemory.destroy(gpu.getDevice());
			
			VkBuffer indices=null;
//			if(indexCount!=0){
//				indices=Vk.createBuffer(gpu.getDevice(), Vk.bufferInfo(stack, size-dataSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT), stack.mallocLong(1));
//				LogUtil.println(size-dataSize);
//				memory.getMemory().bind(gpu.getDevice(), indices, dataSize);
//			}
			
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
			
			Vk.queueSubmit(gpu.getTransferQueue(), VkSubmitInfo.callocStack(stack)
			                                                   .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
			                                                   .pCommandBuffers(BufferUtil.buffSingle(stack, commandBuffer)));
			
			Vk.queueWaitIdle(gpu.getTransferQueue());

//			vkFreeCommandBuffers(gpu.getDevice(), gpu.getTransferPool().get(), commandBuffer);
		
		}
	}
	
	public static VkBufferMemory createBuffer(VkGpu gpu, MemoryStack stack, int size, int usage, int properties){
		
		VkBuffer buffer=Vk.createBuffer(gpu.getDevice(), Vk.bufferInfo(stack, size, usage), stack.mallocLong(1));
		
		VkMemoryRequirements memRequ =buffer.getMemRequirements(gpu, stack);
		VkMemoryAllocateInfo memAlloc=VkMemoryAllocateInfo.callocStack(stack);
		memAlloc.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
		        .allocationSize(Math.max(memRequ.size(), size))
		        .memoryTypeIndex(Vk.findMemoryType(gpu.getMemoryProperties(), memRequ.memoryTypeBits(), properties));
		
		VkBufferMemory data=new VkBufferMemory(buffer, Vk.allocateMemory(gpu.getDevice(), memAlloc, stack.mallocLong(1)), size);
		
		data.getMemory().bind(gpu.getDevice(), data.getBuffer());
		
		return data;
	}
	
	
}
