package com.lapissea.vulkanimpl.memory;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class Chunk{
	List<Block> blocks=new ArrayList<>(3);
	
	void alloc(VkGpu ctx, long size, int type){
		try(MemoryStack stack=stackPush()){
			VkMemoryAllocateInfo info=VkMemoryAllocateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
			    .memoryTypeIndex(type)
			    .allocationSize(size);
			
//			return new Block(VkDeviceMemory.alloc(ctx, info), 0, size);
		}
	}

}
