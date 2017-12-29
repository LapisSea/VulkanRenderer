package com.lapissea.vulkanimpl.model;

import com.lapissea.vulkanimpl.VkImageTexture;
import com.lapissea.vulkanimpl.VkModelFormat;
import com.lapissea.vulkanimpl.model.meta.VkModelMeta;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkModel implements VkDestroyable{
	
	public enum IndexType{
		SHORT(VK_INDEX_TYPE_UINT16, 2),
		INT(VK_INDEX_TYPE_UINT32, 4);
		
		public final int val;
		public final int bytes;
		
		IndexType(int val, int bytes){
			this.val=val;
			this.bytes=bytes;
			//node based scripting material
			//nbsm
		}
	}
	
	private final VkModelFormat  format;
	private final VkBufferMemory memory;
	private final VkModelMeta    meta;
	
	//TODO: Remove this failed abortion and make the fucking shader materials; you lazy asshole
	public VkImageTexture texture;
	
	public VkModel(VkBufferMemory memory, VkModelFormat format, VkModelMeta meta){
		this.memory=memory;
		this.format=format;
		this.meta=meta;
	}
	
	
	@Override
	public void destroy(){
		memory.destroy();
		meta.destroy();
	}
	
	public VkBufferMemory getMemory(){
		return memory;
	}
	
	public VkModelFormat getFormat(){
		return format;
	}
	
	public int getVertexCount(){
		return meta.vertexCount;
	}
	
	public int byteSize(){
		return memory.byteSize();
	}
	
	public int getByteCount(){
		return meta.byteCount;
	}
	
	public VkModelMeta getMeta(){
		return meta;
	}
	
	public void bind(VkCommandBuffer cmd){
		meta.bind(this, cmd);
	}
	
	public void draw(VkCommandBuffer cmd){
		draw(cmd, 1);
	}
	
	public void draw(VkCommandBuffer cmd, int instanceCount){
		meta.draw(cmd, instanceCount);
	}
	
}
