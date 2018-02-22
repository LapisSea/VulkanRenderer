package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.vec.interf.IVec3fR;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.format.VKFormatWriter;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VkModelBuilder{
	
	private static final int DATA_CHUNK_SIZE =1<<6;
	private static final int INDEX_CHUNK_SIZE=1<<6;
	
	private static final LinkedList<SoftReference<byte[]>> DATA_CACHE =new LinkedList<>();
	private static final LinkedList<SoftReference<int[]>>  INDEX_CACHE=new LinkedList<>();
	
	private static byte[] getDataChunk(){
		byte[] result;
		while(!DATA_CACHE.isEmpty()){
			result=DATA_CACHE.getLast().get();
			if(result==null) DATA_CACHE.removeLast();
			else return result;
		}
		return new byte[DATA_CHUNK_SIZE];
	}
	
	private static int[] getIndexChunk(){
		int[] result;
		while(!INDEX_CACHE.isEmpty()){
			result=INDEX_CACHE.getLast().get();
			if(result==null) INDEX_CACHE.removeLast();
			else return result;
		}
		return new int[DATA_CHUNK_SIZE];
	}
	
	
	public final VkModelFormat format;
	private final LinkedList<int[]> indices      =new LinkedList<>(List.of(getIndexChunk()));
	private       int               indexChunkPos=0;
	
	private final LinkedList<byte[]> data        =new LinkedList<>(List.of(getDataChunk()));
	private       int                dataChunkPos=0;
	
	private final byte[]     vertexData;
	private final ByteBuffer vertex;
	private       int        typePos;
	private       int        vertexCount;
	
	public VkModelBuilder(int... parts){
		this(new VkModelFormat(parts));
	}
	
	public VkModelBuilder(VkFormatInfo... parts){
		this(new VkModelFormat(parts));
	}
	
	public VkModelBuilder(VkModelFormat format){
		this.format=format;
		vertex=ByteBuffer.wrap(vertexData=new byte[format.getSize()]).order(ByteOrder.nativeOrder());
	}


//	public ByteBuffer exportIndices(ByteBuffer dest){
//		if(indices.isEmpty()) return dest;
//
//		for(int i1=0;i1<indices.size();i1++){
//			int i=indices.get(i1);
//
//			dest.put((byte)(i&0xFF));
//			dest.put((byte)((i>>8)&0xFF));
//		}
//		indices.clear();
//
//		return dest;
//	}
	
	private void put(float f, VkFormatInfo.Component comp){
		switch(comp.bitSize){
		case 32:{
			vertex.putFloat(f);
			return;
		}
		}
		throw new RuntimeException("unsupported bit size "+comp.bitSize);
	}
	
	
	public VkModelBuilder put1I(int x){
		this.<VKFormatWriter.I>get().write(x);
		return this;
	}
	
	
	public VkModelBuilder put1F(float x){
		this.<VKFormatWriter.F>get().write(x);
		return this;
	}
	
	public VkModelBuilder put2F(float x, float y){
		this.<VKFormatWriter.FF>get().write(x, y);
		return this;
	}
	
	public VkModelBuilder put3F(float x, float y, float z){
		this.<VKFormatWriter.FFF>get().write(x, y, z);
		return this;
	}
	
	public VkModelBuilder put3F(IVec3fR vec){
		return put3F(vec.x(), vec.y(), vec.z());
	}
	
	public VkModelBuilder put4F(float x, float y, float z, float w){
		this.<VKFormatWriter.FFFF>get().write(x, y, z, w);
		return this;
	}
	
	
	private <T extends VKFormatWriter> T get(){
		T t=(T)format.getType(typePos).getWriter();
		t.setDest(vertex);
		typePos++;
		return t;
	}
	
	public void next(){
		if(vertex.capacity()!=vertex.position()) throw new IllegalStateException("Vertex not finished: "+vertex.position()+" out of "+vertex.capacity()+" bytes");
		
		int pos=0;
		
		while(pos<vertexData.length){
			byte[] chunk     =data.getLast();
			int    toTransfer=Math.min(vertexData.length-pos, chunk.length-dataChunkPos);
			System.arraycopy(vertexData, pos, chunk, dataChunkPos, toTransfer);
			dataChunkPos+=toTransfer;
			pos+=toTransfer;
			if(dataChunkPos==chunk.length){
				dataChunkPos=0;
				data.add(getDataChunk());
			}
		}
		typePos=0;
		vertex.position(0);
		vertexCount++;
	}
	
	public int indexCount(){
		return (indices.size()-1)*INDEX_CHUNK_SIZE+indexChunkPos;
	}
	
	public int dataSize(){
		return (data.size()-1)*INDEX_CHUNK_SIZE+dataChunkPos;
	}
	
	
	private void putVertexData(ByteBuffer dest){
		Iterator<byte[]> iter=data.iterator();
		byte[]           chunk;
		if(data.size()>1){
			for(int i=0, j=data.size()-1;i<j;i++){
				dest.put(chunk=iter.next());
				DATA_CACHE.add(new SoftReference<>(chunk));
			}
		}
		dest.put(chunk=iter.next(), 0, dataChunkPos);
		DATA_CACHE.add(new SoftReference<>(chunk));
	}
	
	public VkModel bake(VkGpu gpu){
		if(vertex.position()!=0) throw new IllegalStateException("Vertex not finished ");
		
		int               dataSize  =dataSize();
		int               indexCount=indexCount();
		boolean           indexed   =indexCount>0;
		VkModel.IndexType indexType =indexCount<65535?VkModel.IndexType.SHORT:VkModel.IndexType.INT;
		
		if(indexed) throw new RuntimeException();// bufferInfo.size(bufferInfo.size()+indexCount*indexType.bytes);
		
		VkBuffer       stagingBuffer=gpu.createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, dataSize);
		VkDeviceMemory stagingMemory=stagingBuffer.allocateBufferMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		
		stagingMemory.memorySession(stagingBuffer.size, mem->{
			putVertexData(mem);
			stagingMemory.flushRanges(stagingBuffer.size);
			stagingMemory.invalidateRanges(stagingBuffer.size);
		});
		
		VkBuffer       modelBuffer=gpu.createBuffer(VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, dataSize);
		VkDeviceMemory modelMemory=modelBuffer.allocateBufferMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		return new VkModel.Raw(modelBuffer, modelMemory, format, vertexCount);
	}
}