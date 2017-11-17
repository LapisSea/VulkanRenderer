package com.lapissea.vulkankimpl;

import com.lapissea.vulkankimpl.model.BufferBuilder;

public class VkModelFormat{
	
	private final Class[] parts;
	private final int     sizeBits;
	private final int[]   offsets;
	private final int     hashCode;
	
	private final BufferBuilder[] attributes;
	
	public VkModelFormat(Class... parts){
		this.parts=parts.clone();
		offsets=new int[parts.length];
		attributes=new BufferBuilder[parts.length];
		
		int hash=0;
		int size=0;
		for(int i=0;i<parts.length;i++){
			
			attributes[i]=BufferBuilder.find(getPart(i), i);
			
			offsets[i]=size;
			size+=attributes[i].getSizeBits();
			
			int space=size/Byte.SIZE;
			hash<<=space;
			int ones=0;
			for(int j=0;j<space*Byte.SIZE;j++){
				ones+=Math.pow(1, j);
			}
			hash+=parts[i].hashCode()&ones;
		}
		
		this.sizeBits=size;
		this.hashCode=hash;
	}
	
	public Class getPart(int i){
		return parts[i];
	}
	
	public int getOffset(int i){
		return offsets[i];
	}
	
	/**
	 * get sizeBits of vertex in bits
	 *
	 * @return
	 */
	public int getSizeBits(){
		return sizeBits;
	}
	
	public int getSizeBytes(){
		return getSizeBits()/Byte.SIZE;
	}
	
	public int partCount(){
		return parts.length;
	}
	
	public int getFormat(int i){
		return attributes[i].getFormat();
	}
	
	public BufferBuilder getAttribute(int i){
		return attributes[i];
	}
	
	@Override
	public int hashCode(){
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj){
		return obj instanceof VkModelFormat&&obj.hashCode()==hashCode();
	}
}
