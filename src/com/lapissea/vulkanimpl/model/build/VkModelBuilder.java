package com.lapissea.vulkanimpl.model.build;

import com.lapissea.vulkanimpl.SingleUseCommands;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.VkModelFormat;
import com.lapissea.vulkanimpl.model.BufferBuilder;
import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.model.meta.VkModelMeta;
import com.lapissea.vulkanimpl.model.meta.VkModelMetaIndexed;
import com.lapissea.vulkanimpl.model.meta.VkModelMetaNonIndexed;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelBuilder{
	
	private class TByteArrayListExpose extends TByteArrayList{
		public TByteArrayListExpose(int capacity){
			super(capacity);
		}
		
		byte[] data(){
			return _data;
		}
	}
	
	private class TIntArrayListExpose extends TIntArrayList{
		public TIntArrayListExpose(int capacity){
			super(capacity);
		}
		
		int[] data(){
			return _data;
		}
	}
	
	
	public final  VkModelFormat        format;
	private final TByteArrayListExpose data;
	private final TIntArrayListExpose  indices;
	private final ByteBuffer           vertex;
	
	private int typePos;
	private int vertexCount;
	
	public VkModelBuilder(VkModelFormat format){
		this.format=format;
		vertex=BufferUtils.createByteBuffer(format.getSizeBits()/Byte.SIZE);
		indices=new TIntArrayListExpose(3);
		data=new TByteArrayListExpose(vertex.capacity()*3);
	}
	
	
	private BufferBuilder get(Class attribute){
		if(Vk.DEBUG&&typePos>=format.partCount()) throw new IllegalStateException("Attribute overflow");
		BufferBuilder attr=format.getAttribute(typePos);
		if(Vk.DEBUG&&!attr.checkClass(attribute)) throw new IllegalArgumentException(attr+" can not accept "+attribute.getName());
		typePos++;
		return attr;
	}
	
	public VkModelBuilder put(Object attribute){
		get(attribute.getClass()).put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(boolean attribute){
		get(boolean.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(byte attribute){
		get(byte.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(char attribute){
		get(char.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(short attribute){
		get(short.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(int attribute){
		get(int.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(long attribute){
		get(long.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(float attribute){
		get(float.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder put(double attribute){
		get(double.class);
		BufferBuilder.put(vertex, attribute);
		return this;
	}
	
	public VkModelBuilder done(){
		if(typePos<format.partCount()) throw new IllegalStateException("Unfinished vertex");
		
		typePos=0;
		vertexCount++;
		vertex.position(0);
		
		for(int i=0;i<vertex.capacity();i++){
			data.add(vertex.get(i));
		}
		return this;
	}
	
	public int getIndexCount(){
		return indices.size();
	}

	public ByteBuffer exportData(ByteBuffer dest){
		dest.put(data.toArray());
		data.clear(vertex.capacity()*3);

		return dest;
	}

	public ByteBuffer exportIndices(ByteBuffer dest){
		if(indices.size()==0) return dest;
		
		for(int i1=0;i1<indices.size();i1++){
			int i=indices.get(i1);
			
			dest.put((byte)(i&0xFF));
			dest.put((byte)((i>>8)&0xFF));
		}
		indices.clear(3);
		
		return dest;
	}
	
	public int getVertexCount(){
		return vertexCount;
	}
	
	public int dataSize(){
		return data.size();
	}
	
	public int size(){
		return data.size()+getIndexCount()*getIndexType().bytes;
	}
	
	public void indices(int... indices){
		this.indices.addAll(indices);
	}
	
	public void indices(int id){
		indices.add(id);
	}
	
	public VkModel.IndexType getIndexType(){
		return getIndexCount()<65535?VkModel.IndexType.SHORT:VkModel.IndexType.INT;
	}
	
	public VkModel upload(VkGpu gpu){
		try(MemoryStack stack=stackPush()){
			VkModel.IndexType indexType=getIndexType();
			
			int dataSize  =dataSize();
			int indexCount=getIndexCount();
			int indexSize =indexCount*indexType.bytes;
			int totalSize =dataSize+indexSize;
			
			VkModelMeta meta;
			if(indexCount==0) meta=new VkModelMetaNonIndexed(vertexCount, totalSize);
			else meta=new VkModelMetaIndexed(vertexCount, totalSize, indexCount, indexType, format);
			
			VkBufferMemory stagingMemory=gpu.createBufferMem(totalSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			VkBufferMemory memory       =gpu.createBufferMem(totalSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			
			try(VkBufferMemory.MemorySession ses=stagingMemory.requestMemory(gpu.getDevice())){
//				ses.memory.put(data.data(), 0, data.size());
//				for(int i1=0;i1<indices.size();i1++){
//					int i=indices.get(i1);
//
//					ses.memory.put((byte)(i&0xFF));
//					ses.memory.put((byte)((i>>8)&0xFF));
//				}
				exportData(ses.memory);
				exportIndices(ses.memory);
			}
			
			try(SingleUseCommands commands=new SingleUseCommands(stack, gpu)){
				vkCmdCopyBuffer(commands.commandBuffer, stagingMemory.getBuffer().get(), memory.getBuffer().get(), VkBufferCopy.callocStack(1, commands.stack).size(totalSize));
			}
			
			memory.flushMemory(gpu);
			stagingMemory.destroy();
			gpu.waitIdle();
			return new VkModel(memory, format, meta);
		}
	}
	
}
