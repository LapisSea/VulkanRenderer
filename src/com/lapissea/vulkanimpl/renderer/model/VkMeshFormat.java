package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.util.NotNull;
import com.lapissea.vulkanimpl.util.format.VkFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.ArrayList;
import java.util.Iterator;

import static com.lapissea.util.UtilL.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkMeshFormat implements Iterable<VkFormat>{
	
	private final VkFormat[] types;
	private final boolean[]  disabled;
	
	private int offsets[];
	private int size;
	private int hash;
	
	public VkMeshFormat(int[] types){
		this(convert(types, VkFormat[]::new, VkFormat::get));
	}
	
	public VkMeshFormat(@NotNull VkFormat... types){
		disabled=new boolean[types.length];
		this.types=types;
	}
	
	public boolean isDisabled(int index){
		return disabled[index];
	}
	
	public VkMeshFormat disable(int index){
		if(disabled[index]) return this;
		disabled[index]=true;
		offsets=null;
		return this;
	}
	
	public VkMeshFormat enable(int index){
		if(!disabled[index]) return this;
		disabled[index]=false;
		offsets=null;
		return this;
	}
	
	private void calc(){
		if(offsets!=null) return;
		
		int disabledCount=0;
		for(boolean b : disabled){ if(!b) disabledCount++; }
		
		size=0;
		offsets=new int[disabledCount];
		
		ArrayList<VkFormat> a=new ArrayList<>();
		
		for(int i=0, j=0;i<types.length;i++){
			if(disabled[i]) return;
			
			offsets[j++]=size;
			size+=types[i].totalByteSize;
			a.add(types[i]);
		}
		
		hash=a.hashCode();
		
	}
	
	public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack){
		var input=VkVertexInputBindingDescription.callocStack(1, stack);
		input.get(0)
		     .binding(0)
		     .stride(getSize())
		     .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		return input;
	}
	
	public VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack){
		calc();
		var input=VkVertexInputAttributeDescription.callocStack(offsets.length, stack);
		
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
		calc();
		return size;
	}
	
	public VkFormat getType(int i){
		return types[i];
	}
	
	public int getTypeCount(){
		return types.length;
	}
	
	@Override
	public String toString(){
		StringBuilder result=new StringBuilder("VkMeshFormat{size=").append(getSize()).append(", comps=");
		
		for(int i=0;i<offsets.length;i++){
			var type=types[i];
			
			result.append(offsets[i]).append('+').append(type.totalByteSize).append("_").append(type.name.replaceAll("VK_FORMAT_", ""));
			if(i+1<offsets.length){
				result.append(", ");
			}
		}
		return result.append('}').toString();
	}
	
	@NotNull
	@Override
	public Iterator<VkFormat> iterator(){
		return new Iterator<>(){
			int cursor=0;
			
			@Override
			public boolean hasNext(){
				return cursor<getTypeCount();
			}
			
			@Override
			public VkFormat next(){
				return getType(cursor++);
			}
		};
	}
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(!(o instanceof VkMeshFormat)) return false;
		
		var m=(VkMeshFormat)o;
		if(disabled.length!=m.disabled.length) return false;
		
		for(int i=0, j=0;i<types.length;i++){
			if(disabled[i]!=m.disabled[i]) return false;
			if(disabled[i]) continue;
			if(!types[i].equals(m.types[i])) return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode(){
		calc();
		return hash;
	}
}
