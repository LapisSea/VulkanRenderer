package com.lapissea.vulkanimpl.model;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.simplevktypes.VkBuffer;
import com.lapissea.vulkanimpl.simplevktypes.VkDeviceMemory;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMappedMemoryRange;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkBufferMemory implements VkDestroyable{
	
	private final VkBuffer       buffer;
	private final VkDeviceMemory memory;
	private final int            byteSize;
	
	private VkMappedMemoryRange range;
	
	public VkBufferMemory(VkBuffer buffer, VkDeviceMemory memory, int byteSize){
		this.buffer=buffer;
		this.memory=memory;
		this.byteSize=byteSize;
	}
	
	
	@Override
	public void destroy(){
		buffer.destroy();
		memory.destroy();
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
	
	public class MemorySession implements AutoCloseable{
		private final VkDevice   device;
		public final  ByteBuffer memory;
		private final PointerBuffer pointer=memAllocPointer(1);
		
		private MemorySession(VkDevice device, int offset, long byteSize){
			this.device=device;
			memory=mapMemory(device, pointer, offset, byteSize).getByteBuffer(byteSize());
		}
		
		@Override
		public void close(){
			unmapMemory(device);
			memFree(pointer);
		}
	}
	
	public MemorySession requestMemory(VkGpu gpu){
		return requestMemory(gpu.getDevice());
	}
	
	public MemorySession requestMemory(VkDevice device){
		return requestMemory(device, 0, VK_WHOLE_SIZE);
	}
	
	public MemorySession requestMemory(VkDevice device, int offset, long byteSize){
		return new MemorySession(device, offset, byteSize);
	}
}
