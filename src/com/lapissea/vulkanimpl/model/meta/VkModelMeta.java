package com.lapissea.vulkanimpl.model.meta;

import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VkModelMeta implements VkDestroyable{
	
	public final int vertexCount, byteCount;
	
	public VkModelMeta(int vertexCount, int byteCount){
		this.vertexCount=vertexCount;
		this.byteCount=byteCount;
	}
	
	
	@Override
	public void destroy(){}
	
	public abstract boolean isIndexed();
	
	public abstract void bind(VkModel parent, VkCommandBuffer cmd);
	
	public abstract void draw(VkCommandBuffer cmd, int instanceCount);
	
}