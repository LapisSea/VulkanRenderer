package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VkModel implements VkDestroyable{
	
	private static final LongBuffer ZERO_OFFSET=memAllocLong(1).put(0, 0);
	
	public enum IndexType{
		SHORT(VK_INDEX_TYPE_UINT16, 2),
		INT(VK_INDEX_TYPE_UINT32, 4);
		
		public final int handle;
		public final int bytes;
		
		IndexType(int handle, int bytes){
			this.handle=handle;
			this.bytes=bytes;
			//node based scripting material
			//nbsm
		}
	}
	
	public static class Indexed extends VkModel{
		
		private final int       indexCount;
		private final int       dataSize;
		private final IndexType indexType;
		
		public Indexed(VkBuffer buffer, VkDeviceMemory memory, int dataSize, int indexCount, IndexType indexType){
			super(buffer, memory);
			this.indexCount=indexCount;
			this.dataSize=dataSize;
			this.indexType=indexType;
		}
		
		@Override
		public void bind(VkCommandBufferM cmd){
			vkCmdBindVertexBuffers(cmd, 0, buffer.getBuff(), ZERO_OFFSET);
			vkCmdBindIndexBuffer(cmd, buffer.getHandle(), 0, indexType.handle);
		}
		
		@Override
		public void render(VkCommandBufferM cmd){
			vkCmdDrawIndexed(cmd, indexCount, 1, 0, 0, 0);
		}
		
	}
	
	public static class Raw extends VkModel{
		
		private final int vertexCount;
		
		public Raw(VkBuffer buffer, VkDeviceMemory memory, int vertexCount){
			super(buffer, memory);
			this.vertexCount=vertexCount;
		}
		
		@Override
		public void bind(VkCommandBufferM cmd){
			vkCmdBindVertexBuffers(cmd, 0, buffer.getBuff(), ZERO_OFFSET);
		}
		
		@Override
		public void render(VkCommandBufferM cmd){
			vkCmdDraw(cmd, vertexCount, 1, 0, 0);
		}
	}
	
	protected final VkBuffer       buffer;
	protected final VkDeviceMemory memory;
	
	public VkModel(VkBuffer buffer, VkDeviceMemory memory){
		this.buffer=buffer;
		this.memory=memory;
	}
	
	
	@Override
	public void destroy(){
		memory.destroy();
		buffer.destroy();
	}
	
	public abstract void bind(VkCommandBufferM cmd);
	
	public abstract void render(VkCommandBufferM cmd);
	
}
