package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VkModel implements VkDestroyable{
	
	private static final LongBuffer ZERO_OFFSET=memAllocLong(1).put(0, 0);
	
	public interface IndexWriter{
		void write(ByteBuffer byteBuffer, int id);
	}
	
	public enum IndexType{
		SHORT(VK_INDEX_TYPE_UINT16, 2, (b, i)->b.putShort((short)i)),
		INT(VK_INDEX_TYPE_UINT32, 4, ByteBuffer::putInt);
		
		public final int         handle;
		public final int         bytes;
		public final IndexWriter indexWriter;
		
		IndexType(int handle, int bytes, IndexWriter indexWriter){
			this.handle=handle;
			this.bytes=bytes;
			//node based scripting material
			//nbsm
			this.indexWriter=indexWriter;
		}
	}
	
	public static class Indexed extends VkModel{
		
		private final int       indexCount;
		private final int       indexStart;
		private final IndexType indexType;
		
		public Indexed(VkBuffer buffer, VkDeviceMemory memory, VkModelFormat format, int indexStart, int indexCount, IndexType indexType){
			super(buffer, memory, format);
			this.indexCount=indexCount;
//			this.indexStart=(int)(buffer.size-indexCount*indexType.bytes);
			this.indexStart=indexStart;
			this.indexType=indexType;
		}
		
		@Override
		public void bind(VkCommandBufferM cmd){
			vkCmdBindVertexBuffers(cmd, 0, buffer.getBuff(), ZERO_OFFSET);
			vkCmdBindIndexBuffer(cmd, buffer.getHandle(), indexStart, indexType.handle);
		}
		
		@Override
		public void render(VkCommandBufferM cmd){
			vkCmdDrawIndexed(cmd, indexCount, 1, 0, 0, 0);
		}
		
	}
	
	public static class Raw extends VkModel{
		
		private final int vertexCount;
		
		public Raw(VkBuffer buffer, VkDeviceMemory memory, VkModelFormat format, int vertexCount){
			super(buffer, memory, format);
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
	private final   VkModelFormat  format;
	
	public VkModel(VkBuffer buffer, VkDeviceMemory memory, VkModelFormat format){
		this.buffer=buffer;
		this.memory=memory;
		this.format=format;
	}
	
	
	public VkModelFormat getFormat(){
		return format;
	}
	
	@Override
	public void destroy(){
		memory.destroy();
		buffer.destroy();
	}
	
	public abstract void bind(VkCommandBufferM cmd);
	
	public abstract void render(VkCommandBufferM cmd);
	
}
