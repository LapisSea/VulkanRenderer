package com.lapissea.vulkanimpl.model.meta;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkModelFormat;
import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.simplevktypes.VkBuffer;
import com.lapissea.vulkanimpl.util.DevelopmentMemorySafety;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelMetaIndexed extends VkModelMeta{
	
	private final int indexCount, indexStart;
	private final VkModel.IndexType indexType;
	
	public VkModelMetaIndexed(int vertexCount, int byteCount, int indexCount, VkModel.IndexType indexType, VkModelFormat format){
		super(vertexCount, byteCount);
		this.indexCount=indexCount;
		this.indexType=indexType;
		indexStart=vertexCount*format.getSizeBytes();
	}
	
	@Override
	public boolean isIndexed(){
		return true;
	}
	
	@Override
	public void bind(VkModel parent, VkCommandBuffer cmd){
		VkBuffer buff=parent.getMemory().getBuffer();
		try(MemoryStack stack=stackPush()){
			vkCmdBindVertexBuffers(cmd, 0, stack.longs(buff.get()), stack.longs(0));
			vkCmdBindIndexBuffer(cmd, buff.get(), indexStart, indexType.val);
		}
	}
	
	@Override
	public void draw(VkCommandBuffer cmd, int instanceCount){
		vkCmdDrawIndexed(cmd, indexCount, instanceCount, 0, 0, 0);
	}
	
	
	public VkModel.IndexType getIndexType(){
		return indexType;
	}
	
	public int getIndexCount(){
		return indexCount;
	}
	
}