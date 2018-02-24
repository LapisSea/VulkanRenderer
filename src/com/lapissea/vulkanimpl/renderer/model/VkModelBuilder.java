package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.vec.color.IColorM;
import com.lapissea.vec.interf.IVec3fR;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.renderer.model.VkModel.IndexType;
import com.lapissea.vulkanimpl.util.format.VKFormatWriter;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkCommandPool;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.lapissea.vulkanimpl.renderer.model.VkModel.IndexType.*;
import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelBuilder{
	
	private static final int DATA_CHUNK_SIZE =1<<6;
	private static final int INDEX_CHUNK_SIZE=1<<5;
	
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
	
	public VkModelBuilder put4F(IColorM color){
		return put4F(color.r(), color.g(), color.b(), color.a());
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
			}else break;
		}
		typePos=0;
		vertex.position(0);
		vertexCount++;
	}
	
	public void addIndices(int... indices){
		
		int pos=0;
		
		while(pos<indices.length){
			int[] chunk     =this.indices.getLast();
			int   toTransfer=Math.min(indices.length-pos, chunk.length-indexChunkPos);
			System.arraycopy(indices, pos, chunk, indexChunkPos, toTransfer);
			indexChunkPos+=toTransfer;
			pos+=toTransfer;
			if(indexChunkPos==chunk.length){
				indexChunkPos=0;
				this.indices.add(getIndexChunk());
			}else break;
		}
	}
	
	public long indexCount(){
		return (indices.size()-1L)*INDEX_CHUNK_SIZE+indexChunkPos;
	}
	
	public long dataSize(){
		return (data.size()-1L)*DATA_CHUNK_SIZE+dataChunkPos;
	}
	
	
	public VkModel bake(VkGpu gpu, VkCommandPool transferPool){
		if(vertex.position()!=0) throw new IllegalStateException("Vertex not finished ");
		
		
		long      vertexSize=dataSize();
		long      indexCount=indexCount();
		boolean   indexed   =indexCount>0;
		IndexType indexType;
		int       indexStart;
		long      totalSize;
		
		if(indexed){
			indexType=indexCount<1<<16?SHORT:INT;
			totalSize=vertexSize+indexCount*indexType.bytes;
		}else{
			indexType=null;
			totalSize=vertexSize;
		}
		
		VkBuffer       stagingBuffer=gpu.createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, totalSize);
		VkDeviceMemory stagingMemory=stagingBuffer.allocateBufferMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		try(VkDeviceMemory.MemorySession ses=stagingMemory.memorySession(stagingBuffer.size)){
			ByteBuffer mem=ses.memory;
			writeVertex(mem);
			
			if(indexed){
				indexStart=mem.position();
				writeIndex(mem, indexType);
			}else indexStart=-1;
			
			stagingMemory.flushRanges();
			stagingMemory.invalidateRanges();
		}
		
		int usage=VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
		if(indexed) usage|=VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
		
		VkBuffer       modelBuffer=gpu.createBuffer(usage, totalSize);
		VkDeviceMemory modelMemory=modelBuffer.allocateBufferMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		modelBuffer.copyFrom(stagingBuffer, 0, transferPool);
		
		stagingBuffer.destroy();
		stagingMemory.destroy();
		
		gpu.waitIdle();
		
		
		if(indexed) return new VkModel.Indexed(modelBuffer, modelMemory, format, indexStart, (int)indexCount, indexType);
		return new VkModel.Raw(modelBuffer, modelMemory, format, vertexCount);
	}
	
	private void writeVertex(ByteBuffer mem){
		
		Iterator<byte[]> iter=data.iterator();
		if(data.size()>1){
			for(int i=0, j=data.size()-1;i<j;i++){
				mem.put(iter.next());
			}
		}
		mem.put(iter.next(), 0, dataChunkPos);
		
		data.stream().map(SoftReference::new).forEach(DATA_CACHE::add);
		data.clear();
	}
	
	private void writeIndex(ByteBuffer mem, IndexType indexType){
		Iterator<int[]> iter=indices.iterator();
		if(indices.size()>1){
			for(int i=0, j=indices.size()-1;i<j;i++){
				int[] chunk=iter.next();
				for(int k=0;k<INDEX_CHUNK_SIZE;k++){
					if(DEV_ON){
						if(chunk[k]<0) throw new IndexOutOfBoundsException("Index must be positive!");
						if(chunk[k]>=vertexCount) throw new IndexOutOfBoundsException("Index must be less than "+vertexCount+"!");
					}
					indexType.indexWriter.write(mem, chunk[k]);
				}
				
			}
		}
		int[] chunk=iter.next();
		for(int k=0;k<indexChunkPos;k++){
			if(DEV_ON){
				if(chunk[k]<0) throw new IndexOutOfBoundsException("Index must be positive!");
				if(chunk[k]>=vertexCount) throw new IndexOutOfBoundsException("Index must be less than "+vertexCount+"!");
			}
			indexType.indexWriter.write(mem, chunk[k]);
		}
		indices.stream().map(SoftReference::new).forEach(INDEX_CACHE::add);
		indices.clear();
	}
	
}