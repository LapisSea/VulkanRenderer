package com.lapissea.vulkanimpl.renderer.model;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

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
		vertex=ByteBuffer.allocate(format.getSize());
		indices=new TIntArrayListExpose(3);
		data=new TByteArrayListExpose(vertex.capacity()*3);
	}
	
	
	public ByteBuffer exportIndices(ByteBuffer dest){
		if(indices.isEmpty()) return dest;
		
		for(int i1=0;i1<indices.size();i1++){
			int i=indices.get(i1);
			
			dest.put((byte)(i&0xFF));
			dest.put((byte)((i>>8)&0xFF));
		}
		indices.clear(3);
		
		return dest;
	}
	
	public VkModelBuilder putF(float f){
		format.
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
	
	public int getIndexCount(){
		return indices.size();
	}
	
	public VkModel.IndexType getIndexType(){
		return getIndexCount()<65535?VkModel.IndexType.SHORT:VkModel.IndexType.INT;
	}
}