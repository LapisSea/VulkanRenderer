package com.lapissea.vulkankimpl.model;

import com.lapissea.vulkankimpl.Vk;
import com.lapissea.vulkankimpl.VkGpu;
import com.lapissea.vulkankimpl.simplevktypes.VkBuffer;
import com.lapissea.vulkankimpl.simplevktypes.VkDeviceMemory;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMappedMemoryRange;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class VkBufferMemory{
	
	private final VkBuffer       buffer;
	private final VkDeviceMemory memory;
	private final int            byteSize;
	
	private VkMappedMemoryRange range;
	
	public VkBufferMemory(VkBuffer buffer, VkDeviceMemory memory, int byteSize){
		this.buffer=buffer;
		this.memory=memory;
		this.byteSize=byteSize;
	}
	
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		buffer.destroy(device);
		memory.destroy(device);
		if(range!=null){
			range.free();
			range=null;
		}
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
	
	public PointerBuffer mapMemory(VkDevice device, MemoryStack stack, int offset, long byteSize){
		return mapMemory(device, stack.mallocPointer(1), offset, byteSize);
	}
	
	public PointerBuffer mapMemory(VkDevice device, PointerBuffer dest){
		return mapMemory(device, dest, 0, VK_WHOLE_SIZE);
	}
	
	public PointerBuffer mapMemory(VkDevice device, PointerBuffer dest, int offset, long byteSize){
		return Vk.mapMemory(device, getMemory(), offset, byteSize, 0, dest);
	}
	
	private void unmapMemory(VkDevice device){
		Vk.unmapMemory(device, getMemory());
	}
	
	public void flushMemory(VkGpu gpu){
		flushMemory(gpu.getDevice());
	}
	
	public void flushMemory(VkDevice device){
		if(range==null){
			range=VkMappedMemoryRange.calloc();
			range.memory(getMemory().get())
			     .size(byteSize());
		}
		
		Vk.flushMappedMemoryRanges(device, range);
		Vk.invalidateMappedMemoryRanges(device, range);
	}
	
	public void requestMemory(VkGpu gpu, MemoryStack stack, Consumer<ByteBuffer> consumer){
		requestMemory(gpu.getDevice(), stack, consumer);
	}
	
	public void requestMemory(VkGpu gpu, PointerBuffer dest, Consumer<ByteBuffer> consumer){
		requestMemory(gpu.getDevice(), dest, consumer);
	}
	
	public void requestMemory(VkDevice device, MemoryStack stack, Consumer<ByteBuffer> consumer){
		PointerBuffer pointer=mapMemory(device, stack);
		consumer.accept(pointer.getByteBuffer(byteSize()));
		unmapMemory(device);
	}
	
	public void requestMemory(VkDevice device, PointerBuffer dest, Consumer<ByteBuffer> consumer){
		PointerBuffer pointer=mapMemory(device, dest);
		consumer.accept(pointer.getByteBuffer(byteSize()));
		unmapMemory(device);
	}
	
	public void requestMemory(VkDevice device, PointerBuffer dest, int offset, long byteSize, Consumer<ByteBuffer> consumer){
		PointerBuffer pointer=mapMemory(device, dest, offset, byteSize);
		consumer.accept(pointer.getByteBuffer(byteSize()));
		unmapMemory(device);
	}
}
