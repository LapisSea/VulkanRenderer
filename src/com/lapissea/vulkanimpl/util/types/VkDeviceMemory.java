package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkMappedMemoryRange;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceMemory implements VkGpuCtx, VkDestroyable{
	
	public class MemorySession implements AutoCloseable{
		public final  ByteBuffer    memory;
		private final PointerBuffer pointer;
		private final long          offset;
		private final long          byteSize;
		private final boolean       internalPointer;
		
		private MemorySession(PointerBuffer pointer, long offset, long byteSize, boolean internalPointer){
			this.pointer=pointer;
			this.offset=offset;
			this.byteSize=byteSize;
			this.internalPointer=internalPointer;
			
			memory=map(offset, byteSize, pointer).getByteBuffer((int)byteSize);
		}
		
		@Override
		public void close(){
			unmap();
			if(!internalPointer) memFree(pointer);
		}
	}
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	private final VkMappedMemoryRange range=VkMappedMemoryRange.calloc().sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE);
	public final long size;
	
	public VkDeviceMemory(VkGpuCtx gpuCtx, LongBuffer dest, long size){
		gpu=gpuCtx.getGpu();
		handle=dest;
		range.memory(handle.get(0));
		this.size=size;
	}
	
	@Override
	public void destroy(){
		vkFreeMemory(getDevice(), handle.get(0), null);
		range.free();
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public void bindBuffer(VkBuffer modelBuffer){
		vkBindBufferMemory(getDevice(), modelBuffer.getHandle(), handle.get(0), 0);
	}
	
	public PointerBuffer map(long offset, long size, PointerBuffer pp){
		return Vk.mapMemory(getDevice(), handle.get(0), offset, size, 0, pp);
	}
	
	public void unmap(){
		vkUnmapMemory(getDevice(), handle.get(0));
	}
	
	public void flushRanges(){
		flushRanges(VK_WHOLE_SIZE);
	}
	
	public void flushRanges(long size){
		Vk.flushMappedMemoryRanges(getDevice(), range.size(size));
	}
	
	public void invalidateRanges(){
		invalidateRanges(VK_WHOLE_SIZE);
	}
	
	public void invalidateRanges(long size){
		Vk.invalidateMappedMemoryRanges(getDevice(), range.size(size));
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void memorySession(long byteSize, Consumer<ByteBuffer> memoryConsumer){
		memorySession(0, byteSize, memoryConsumer);
	}
	
	public void memorySession(long offset, long byteSize, Consumer<ByteBuffer> memoryConsumer){
		PointerBuffer pointer=memAllocPointer(1);
		try{
			memorySession(pointer, offset, byteSize, memoryConsumer);
		}finally{
			memFree(pointer);
		}
	}
	
	public void memorySession(PointerBuffer pointer, long byteSize, Consumer<ByteBuffer> memoryConsumer){
		memorySession(pointer, 0, byteSize, memoryConsumer);
	}
	
	public void memorySession(PointerBuffer pointer, long offset, long byteSize, Consumer<ByteBuffer> memoryConsumer){
		try{
			memoryConsumer.accept(map(offset, byteSize, pointer).getByteBuffer((int)byteSize));
		}finally{
			unmap();
		}
	}
	
	public MemorySession memorySession(long byteSize){
		return new MemorySession(memAllocPointer(1), 0, byteSize, false);
	}
	
	public MemorySession memorySession(long offset, long byteSize){
		return new MemorySession(memAllocPointer(1), offset, byteSize, false);
	}
	
	public MemorySession memorySession(PointerBuffer pointer, long byteSize){
		return new MemorySession(pointer, 0, byteSize, true);
	}
	
	public MemorySession memorySession(PointerBuffer pointer, long offset, long byteSize){
		return new MemorySession(pointer, offset, byteSize, true);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}
