package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.assets.ResourcePool;
import com.lapissea.vulkanimpl.assets.TextureNode;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.format.VkDescriptor;
import com.lapissea.vulkanimpl.util.types.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VkMesh implements VkDestroyable{
	
	private static final LongBuffer ZERO_OFFSET=memAllocLong(1).put(0, 0);
	
	public static final VkMesh.Empty EMPTY_MESH=new Empty();
	
	
	public interface IndexWriter{
		void write(ByteBuffer byteBuffer, int id);
	}
	
	public enum IndexType{
		SHORT(VK_INDEX_TYPE_UINT16, 2, (b, i)->b.putShort((short)i)),
		INT(VK_INDEX_TYPE_UINT32, 4, ByteBuffer::putInt);
		
		public final int         handle;
		public final int         bytes;
		public final IndexWriter writer;
		
		IndexType(int handle, int bytes, IndexWriter writer){
			this.handle=handle;
			this.bytes=bytes;
			//node based scripting material
			//nbsm
			this.writer=writer;
		}
		
		public static IndexType get(int indexCount){
			return indexCount<1<<16?SHORT:INT;
		}
	}
	
	public static final class Empty extends VkMesh{
		
		private Empty(){
			super(null, null, null);
		}
		
		@Override
		public void bind(VkCommandBufferM cmd){
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void render(VkCommandBufferM cmd){
			throw new UnsupportedOperationException();
		}
	}
	
	public static class Indexed extends VkMesh{
		
		private final int       indexCount;
		private final int       indexStart;
		private final IndexType indexType;
		
		public Indexed(VkBuffer buffer, VkDeviceMemory memory, VkMeshFormat format, int indexStart, int indexCount, IndexType indexType){
			super(buffer, memory, format);
			this.indexCount=indexCount;
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
		
		@Override
		public String toString(){
			return String.format("VkMesh{Ind, count=%d, 0x%05X}", indexCount, memory.getHandle());
		}
	}
	
	public static class Raw extends VkMesh{
		
		private final int vertexCount;
		
		public Raw(VkBuffer buffer, VkDeviceMemory memory, VkMeshFormat format, int vertexCount){
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
		
		@Override
		public String toString(){
			return String.format("VkMesh{Raw, count=%d, 0x%05X}", vertexCount, memory.getHandle());
		}
	}
	
	protected final VkBuffer        buffer;
	protected final VkDeviceMemory  memory;
	private final   VkMeshFormat    format;
	private         VkDescriptorSet descriptor;
	
	public VkMaterial material;
	
	public VkMesh(VkBuffer buffer, VkDeviceMemory memory, VkMeshFormat format){
		this.buffer=buffer;
		this.memory=memory;
		this.format=format;
	}
	
	
	public VkMeshFormat getFormat(){
		return format;
	}
	
	static VkTexture empty=null;
	
	public VkDescriptorSet getDescriptor(VkDescriptorPool pool, VkDescriptorSetLayout layout, ResourcePool<VkTexture> texturePool){
		if(descriptor==null){
			VkDescriptor[] poolParts=layout.getParts().clone();
			
			descriptor=pool.allocateDescriptorSets(layout);
			
			poolParts[2]=poolParts[2].withTexture(empty(pool.getGpu()));
			poolParts[3]=poolParts[3].withTexture(empty(pool.getGpu()));
			
			descriptor.update(poolParts);
			
			var gpu=pool.getGpu();
			CompletableFuture.supplyAsync(()->texturePool.get(material.getDiffuse()))
			                 .thenAccept(d->poolParts[2]=poolParts[2].withTexture(d==null?empty(gpu):d))
			                 .thenRun(()->{
				                 synchronized(descriptor){
					                 descriptor.update(poolParts);
				                 }
			                 })
			                 .thenRunAsync(()->pool.getGpu().getInstance().commandBuffersDirty=true, gpu.getInstance());
			
			CompletableFuture.supplyAsync(()->texturePool.get(material.getNormal()))
			                 .thenAccept(n->poolParts[3]=poolParts[3].withTexture(n==null?empty(gpu):n))
			                 .thenRun(()->{
				                 synchronized(descriptor){
					                 descriptor.update(poolParts);
				                 }
			                 })
			                 .thenRunAsync(()->pool.getGpu().getInstance().commandBuffersDirty=true, gpu.getInstance());

//			CompletableFuture.allOf(CompletableFuture.supplyAsync(()->texturePool.get(material.getDiffuse())).thenAccept(d->poolParts[2]=poolParts[2].withTexture(d==null?empty(gpu):d)),
//			                        CompletableFuture.supplyAsync(()->texturePool.get(material.getNormal())).thenAccept(n->poolParts[3]=poolParts[3].withTexture(n==null?empty(gpu):n)))
//			                 .thenRun(()->descriptor.update(poolParts))
//			                 .thenAcceptAsync(e->pool.getGpu().getInstance().commandBuffersDirty=true, gpu.getInstance());
			
		}
		return descriptor;
	}
	
	protected static synchronized VkTexture empty(VkGpu gpu){
		if(empty==null){
			BufferedImage bimg=new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
			empty=TextureNode.bufferedImageToGpu(bimg, gpu);
		}
		return empty;
	}
	
	@Override
	public void destroy(){
		descriptor.destroy();
		memory.destroy();
		buffer.destroy();
	}
	
	public abstract void bind(VkCommandBufferM cmd);
	
	public abstract void render(VkCommandBufferM cmd);
	
}
