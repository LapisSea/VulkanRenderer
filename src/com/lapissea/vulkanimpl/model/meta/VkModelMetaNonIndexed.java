package com.lapissea.vulkanimpl.model.meta;

import com.lapissea.vulkanimpl.model.VkModel;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelMetaNonIndexed extends VkModelMeta{
	
	private final LongBuffer offest=memAllocLong(1).put(0, 0);
	
	public VkModelMetaNonIndexed(int vertexCount, int byteCount){
		super(vertexCount, byteCount);
	}
	
	@Override
	public boolean isIndexed(){
		return false;
	}
	
	@Override
	public void bind(VkModel parent, VkCommandBuffer cmd){
		vkCmdBindVertexBuffers(cmd, 0, parent.getMemory().getBuffer().pointer(), offest);
	}
	
	@Override
	public void draw(VkCommandBuffer cmd, int instanceCount){
		vkCmdDraw(cmd, vertexCount, instanceCount, 0, 0);
	}
	
	@Override
	public void destroy(){
		memFree(offest);
	}
}