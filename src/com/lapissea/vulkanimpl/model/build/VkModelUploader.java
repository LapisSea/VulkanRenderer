package com.lapissea.vulkanimpl.model.build;

import com.lapissea.vulkanimpl.SingleUseCommands;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.model.VkModel;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelUploader{
	
	public static VkModel upload(VkGpu gpu, VkModelBuilder modelBuilder){
		try(MemoryStack stack=stackPush()){
			int size=modelBuilder.size(),
				dataSize=modelBuilder.dataSize(),
				indexCount=modelBuilder.getIndexCount();
			boolean indices16Bit=modelBuilder.indices16Bit();
			
			VkBufferMemory stagingMemory=gpu.createBufferMem(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			VkBufferMemory memory       =gpu.createBufferMem(size, VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			
			try(VkBufferMemory.MemorySession ses=stagingMemory.requestMemory(gpu.getDevice(), stack)){
				modelBuilder.exportData(ses.memory);
				modelBuilder.exportIndices(ses.memory);
			}
			
			try(SingleUseCommands commands=new SingleUseCommands(stack, gpu)){
				vkCmdCopyBuffer(commands.commandBuffer, stagingMemory.getBuffer().get(), memory.getBuffer().get(), VkBufferCopy.callocStack(1, commands.stack).size(size));
			}
			
			memory.flushMemory(gpu);
			stagingMemory.destroy(gpu.getDevice());
			gpu.waitIdle();
			
			return new VkModel(memory, modelBuilder.format, dataSize, indices16Bit?VK_INDEX_TYPE_UINT16:VK_INDEX_TYPE_UINT32, indexCount!=0?indexCount:size/modelBuilder.format.getSizeBytes());
		}
	}
	
}
