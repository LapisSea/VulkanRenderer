package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.util.VkFormatInfo;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VkModelBuilder{
	
	private static final int DATA_CHUNK_SIZE=1<<6;
	
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
	
	public VkModelBuilder(VkModelFormat format){
		this.format=format;
		vertex=ByteBuffer.wrap(vertexData=new byte[format.getSize()]);
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
	
	public VkModelBuilder putF(float x){
		VkFormatInfo i=getAndCheck(1);
		put(x, i.components.get(0));
		typePos++;
		return this;
	}
	
	public VkModelBuilder putF(float x, float y){
		VkFormatInfo i=getAndCheck(2);
		put(x, i.components.get(0));
		put(y, i.components.get(1));
		typePos++;
		return this;
	}
	
	public VkModelBuilder putF(float x, float y, float z){
		VkFormatInfo i=getAndCheck(3);
		put(x, i.components.get(0));
		put(y, i.components.get(1));
		put(z, i.components.get(2));
		typePos++;
		return this;
	}
	
	public VkModelBuilder putF(float x, float y, float z, float w){
		VkFormatInfo i=getAndCheck(3);
		put(x, i.components.get(0));
		put(y, i.components.get(1));
		put(z, i.components.get(2));
		put(w, i.components.get(3));
		typePos++;
		return this;
	}
	
	private VkFormatInfo getAndCheck(int size){
		VkFormatInfo i=format.getType(typePos);
		if(i.components.size()!=size) throw new IllegalArgumentException(i.name+" needs "+i.components.size()+" values");
		return i;
	}
	
	public void next(){
		vertexCount++;
		int pos=0;
		
		while(pos<vertexData.length){
			byte[] chunk     =data.getLast();
			int    toTransfer=Math.min(vertexData.length, chunk.length-dataChunkPos);
			System.arraycopy(vertexData, pos, chunk, dataChunkPos, toTransfer);
			dataChunkPos+=toTransfer;
			pos+=toTransfer;
			if(dataChunkPos==chunk.length){
				dataChunkPos=0;
				pos=0;
				data.add(getDataChunk());
			}
		}
		typePos=0;
		vertex.position(0);
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

//	public void indices(int... indices){
//		this.indices.addAll(indices);
//	}
//
//	public void indices(int id){
//		indices.add(id);
//	}
	
	public int getIndexCount(){
		return indices.size();
	}
	
	public VkModel.IndexType getIndexType(){
		return getIndexCount()<65535?VkModel.IndexType.SHORT:VkModel.IndexType.INT;
	}
	
}