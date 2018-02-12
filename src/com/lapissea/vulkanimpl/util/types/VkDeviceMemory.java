package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceMemory implements VkGpuCtx, VkDestroyable{
	
	public class MemorySession implements AutoCloseable{
		public final  ByteBuffer    memory;
		private final PointerBuffer pointer;
		private final long          offset;
		private final long          byteSize;
		private final boolean       internalPointer;
		
		public MemorySession(long byteSize){
			this(memAllocPointer(1), 0, byteSize, false);
		}
		
		public MemorySession(long offset, long byteSize){
			this(memAllocPointer(1), offset, byteSize, false);
		}
		
		public MemorySession(PointerBuffer pointer, long byteSize){
			this(pointer, 0, byteSize, true);
		}
		
		public MemorySession(PointerBuffer pointer, long offset, long byteSize){
			this(pointer, offset, byteSize, true);
		}
		
		private MemorySession(PointerBuffer pointer, long offset, long byteSize, boolean internalPointer){
			this.pointer=pointer;
			this.offset=offset;
			this.byteSize=byteSize;
			this.internalPointer=internalPointer;
			
			memory=map(offset, byteSize, pointer).getByteBuffer((int)byteSize);
		}
		
		@Override
		public void close(){
			unmap(offset, byteSize, pointer);
			if(!internalPointer) memFree(pointer);
		}
	}
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	
	public VkDeviceMemory(VkGpuCtx gpuCtx, LongBuffer dest){
		gpu=gpuCtx.getGpu();
		handle=dest;
	}
	
	@Override
	public void destroy(){
		vkFreeMemory(getDevice(), handle.get(0), null);
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
	
	public PointerBuffer unmap(long offset, long size, PointerBuffer pp){
		return Vk.mapMemory(getDevice(), handle.get(0), offset, size, 0, pp);
	}
}
