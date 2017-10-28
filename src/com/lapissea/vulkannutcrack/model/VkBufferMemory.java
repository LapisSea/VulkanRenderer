package com.lapissea.vulkannutcrack.model;

import com.lapissea.vulkannutcrack.Vk;
import com.lapissea.vulkannutcrack.simplevktypes.VkBuffer;
import com.lapissea.vulkannutcrack.simplevktypes.VkDeviceMemory;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class VkBufferMemory{
	
	private final VkBuffer       buffer;
	private final VkDeviceMemory memory;
	private final int            byteSize;
	
	public VkBufferMemory(VkBuffer buffer, VkDeviceMemory memory, int byteSize){
		this.buffer=buffer;
		this.memory=memory;
		this.byteSize=byteSize;
	}
	
	
	public void destroy(VkDevice device){
		buffer.destroy(device);
		memory.destroy(device);
	}
	
	public VkBuffer getBuffer(){
		return buffer;
	}
	
	public VkDeviceMemory getMemory(){
		return memory;
	}
	
	public int byteSize(){
		return byteSize;
	}
	
	public PointerBuffer mapMemory(VkDevice device, MemoryStack stack){
		return mapMemory(device, stack.mallocPointer(1));
	}
	
	public PointerBuffer mapMemory(VkDevice device, PointerBuffer dest){
		return Vk.mapMemory(device, getMemory(), 0, VK_WHOLE_SIZE, 0, dest);
	}
	
	private void unmapMemory(VkDevice device){
		Vk.unmapMemory(device, getMemory());
	}
	
	public void requestMemory(VkDevice device, MemoryStack stack, Consumer<ByteBuffer> consumer){
		PointerBuffer pointer=mapMemory(device, stack);
		consumer.accept(pointer.getByteBuffer(byteSize()));
		unmapMemory(device);
	}
}
