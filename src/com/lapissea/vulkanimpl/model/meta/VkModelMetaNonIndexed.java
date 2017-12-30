package com.lapissea.vulkanimpl.model.meta;

import com.lapissea.vulkanimpl.model.VkModel;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelMetaNonIndexed extends VkModelMeta{
	
	public VkModelMetaNonIndexed(int vertexCount, int byteCount){
		super(vertexCount, byteCount);
	}
	
	@Override
	public boolean isIndexed(){
		return false;
	}
	
	@Override
	public void bind(VkModel parent, VkCommandBuffer cmd){
		try(MemoryStack stack=stackPush()){
			vkCmdBindVertexBuffers(cmd, 0, parent.getMemory().getBuffer().pointer(), stack.longs(0));
		}
	}
	
	@Override
	public void draw(VkCommandBuffer cmd, int instanceCount){
		vkCmdDraw(cmd, vertexCount, instanceCount, 0, 0);
	}
	
}