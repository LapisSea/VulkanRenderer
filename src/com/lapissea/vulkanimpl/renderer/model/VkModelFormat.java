package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static com.lapissea.util.UtilL.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkModelFormat{
	
	private final int            size;
	private final int            offsets[];
	private final VkFormatInfo[] types;
	
	public VkModelFormat(int... types){
		this(convert(types, VkFormatInfo[]::new, VkFormatInfo::get));
	}
	
	public VkModelFormat(VkFormatInfo... types){
		this.types=types;
		int siz=0;
		offsets=new int[types.length];
		for(int i=0;i<types.length;i++){
			offsets[i]=siz;
			siz+=types[i].totalByteSize;
		}
		size=siz;
	}
	
	public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack){
		VkVertexInputBindingDescription.Buffer input=VkVertexInputBindingDescription.callocStack(1, stack);
		input.get(0)
		     .binding(0)
		     .stride(size)
		     .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		return input;
	}
	
	public VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack){
		VkVertexInputAttributeDescription.Buffer input=VkVertexInputAttributeDescription.callocStack(offsets.length, stack);
		
		for(int i=0;i<offsets.length;i++){
			input.get(i)
			     .binding(0)//watch out!
			     .location(i)
			     .format(types[i].handle)
			     .offset(offsets[i]);
		}
		
		return input;
	}
	
	public int getSize(){
		return size;
	}
	
	public VkFormatInfo getType(int i){
		return types[i];
	}
	
	public int getTypeCount(){
		return types.length;
	}
	
}
