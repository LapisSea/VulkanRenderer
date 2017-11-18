package com.lapissea.vulkanimpl.model;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.VkModelFormat;

public class VkModel{
	
	private final VkModelFormat format;
	
	private final VkBufferMemory memory;
	private final int            vertexCount;
	
	private final int dataSize;
	private final int indexFormat;
	
	
	public VkModel(VkBufferMemory memory, VkModelFormat format, int dataSize, int indexFormat, int vertexCount){
		this.memory=memory;
		this.format=format;
		
		this.vertexCount=vertexCount;
		
		this.dataSize=dataSize;
		this.indexFormat=indexFormat;
		
	}
	
	
	public void destroy(VkGpu gpu){
		memory.destroy(gpu);
	}
	
	public int getVertexCount(){
		return vertexCount;
	}
	
	public int byteSize(){
		return memory.byteSize();
	}
	
	public VkBufferMemory getMemory(){
		return memory;
	}
	
	public VkModelFormat getFormat(){
		return format;
	}
	
	public int getDataSize(){
		return dataSize;
	}
	
	public int getIndexFormat(){
		return indexFormat;
	}
}
