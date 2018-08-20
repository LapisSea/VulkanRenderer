package com.lapissea.vulkanimpl.util.format;

import com.lapissea.vulkanimpl.VkUniform;
import com.lapissea.vulkanimpl.util.types.VkDescriptorSet;
import com.lapissea.vulkanimpl.util.types.VkTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;

import static com.lapissea.util.UtilL.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDescriptor{
	
	public static final VkDescriptor FRAGMENT_IMAGE       =new VkDescriptor(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
	public static final VkDescriptor FRAGMENT_UNIFORM_BUFF=new VkDescriptor(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
	public static final VkDescriptor VERTEX_UNIFORM_BUFF  =new VkDescriptor(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
	
	public static VkWriteDescriptorSet.Buffer createWriteDescriptorSet(VkDescriptor[] descriptors, VkDescriptorSet dstSet, MemoryStack stack){
		
		VkWriteDescriptorSet.Buffer descriptorWrite=VkWriteDescriptorSet.callocStack(descriptors.length, stack);
		for(int i=0;i<descriptors.length;i++){
			var descriptor=descriptorWrite.get(i);
			var part      =descriptors[i];
			
			descriptor.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			          .dstSet(dstSet.getHandle())
			          .dstBinding(i)
			          .dstArrayElement(0)
			          .descriptorType(part.type);
			
			part.put(descriptor, stack);
			
		}
		return descriptorWrite;
	}
	
	public final int type;
	public final int stageFlags;
	
	private final VkUniform uniform;
	private final VkTexture texture;
	
	public VkDescriptor(int type, int stageFlags){
		this(type, stageFlags, null, null);
	}
	
	public VkDescriptor(int type, int stageFlags, VkUniform uniform, VkTexture texture){
		this.type=type;
		this.stageFlags=stageFlags;
		this.uniform=uniform;
		this.texture=texture;
	}
	
	private boolean isBuffer(){
		return checkFlag(type, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
	}
	
	private boolean isImage(){
		return checkFlag(type, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
	}
	
	public VkDescriptor withUniform(VkUniform uniform){
		if(!isBuffer()) throw new RuntimeException("Can't use uniform in non uniform buffer descriptor!");
		return new VkDescriptor(type, stageFlags, uniform, texture);
	}
	
	public VkDescriptor withTexture(VkTexture texture){
		if(!isImage()) throw new RuntimeException("Can't use texture in non image descriptor!");
		return new VkDescriptor(type, stageFlags, uniform, texture);
	}
	
	public void put(VkWriteDescriptorSet descriptor, MemoryStack stack){
		if(isBuffer()) putUni(descriptor, stack);
		else if(isImage()) putTex(descriptor, stack);
	}
	
	protected void putUni(VkWriteDescriptorSet descriptor, MemoryStack stack){
		Objects.requireNonNull(uniform);
		descriptor.pBufferInfo(uniform.put(VkDescriptorBufferInfo.callocStack(1, stack), 0));
	}
	
	protected void putTex(VkWriteDescriptorSet descriptor, MemoryStack stack){
		Objects.requireNonNull(texture);
		descriptor.pImageInfo(texture.put(VkDescriptorImageInfo.callocStack(1, stack), 0));
	}
	
	@Override
	public String toString(){
		return "VkDescriptor{"+
		       "type="+type+
		       ", stageFlags="+stageFlags+
		       '}';
	}
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(!(o instanceof VkDescriptor)) return false;
		VkDescriptor that=(VkDescriptor)o;
		return type==that.type&&
		       stageFlags==that.stageFlags;
	}
	
	@Override
	public int hashCode(){
		int result=1;
		
		result=31*result+type;
		result=31*result+stageFlags;
		
		return result;
	}
}

