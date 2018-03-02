package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.types.VkBuffer;
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class VkUniform implements VkDestroyable{
	
	private VkBuffer             buffer;
	private VkDeviceMemory       memory;
	private Consumer<ByteBuffer> writeBufferData;
	
	public VkUniform(VkGpu gpu, int size, Consumer<ByteBuffer> writeBufferData){
		this.writeBufferData=writeBufferData;
		buffer=gpu.createBuffer(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, size);
		memory=buffer.createMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
	}
	
	public void updateBuffer(){
		memory.memorySession(buffer.size, writeBufferData);
	}
	
	public void put(VkDescriptorBufferInfo info){
		info.buffer(buffer.getHandle())
		    .offset(0)
		    .range(buffer.size);
	}
	
	@Override
	public void destroy(){
		buffer.destroy();
		memory.destroy();
	}
}
