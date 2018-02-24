package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.SingleUseCommandBuffer;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer implements VkGpuCtx, VkDestroyable{
	
	private final VkGpu      gpu;
	private final LongBuffer handle;
	public final  long       size;
	
	public VkBuffer(VkGpuCtx gpuCtx, LongBuffer dest, long size){
		gpu=gpuCtx.getGpu();
		handle=dest;
		this.size=size;
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyBuffer(getDevice(), handle.get(0), null);
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public LongBuffer getBuff(){
		return handle;
	}
	
	public VkDeviceMemory allocateBufferMemory(int requestedProperties){
		try(VkMemoryRequirements memRequirements=VkMemoryRequirements.malloc()){
			VkDeviceMemory mem=getGpu().allocateMemory(gpu.getMemRequirements(memRequirements, this), size, requestedProperties);
			mem.bindBuffer(this);
			return mem;
		}
	}
	
	public void copyFrom(VkBuffer src, long srcOffset, VkCommandPool transferPool){
		copyFrom(src, srcOffset, 0, size, transferPool);
	}
	
	public void copyFrom(VkBuffer src, long srcOffset, long destOffset, long size, VkCommandPool transferPool){
		try(SingleUseCommandBuffer cmd=new SingleUseCommandBuffer(getGpu().getTransferQueue(), transferPool)){
			copyFrom(src, srcOffset, destOffset, size, cmd.commandBuffer);
		}
	}
	
	public void copyFrom(VkBuffer src, long srcOffset, VkCommandBuffer cmd){
		copyFrom(src, srcOffset, 0, size, cmd);
	}
	
	public void copyFrom(VkBuffer src, long srcOffset, long destOffset, long size, VkCommandBuffer cmd){
		
		if(DEV_ON){
			if(srcOffset<0) throw new IndexOutOfBoundsException("Source offset can't be negative!");
			if(destOffset<0) throw new IndexOutOfBoundsException("Destination offset can't be negative!");
			if(size>this.size) throw new IndexOutOfBoundsException("Size has to be less or equal to destination size!");
			if(size>src.size) throw new IndexOutOfBoundsException("Size has to be less or equal to source size!");
			if(srcOffset+size>this.size) throw new IndexOutOfBoundsException("Destination range of "+srcOffset+" - "+(srcOffset+size)+" is out of range!");
			if(srcOffset+size>src.size) throw new IndexOutOfBoundsException("Source range of "+srcOffset+" - "+(srcOffset+size)+" is out of range!");
		}
		
		try(VkBufferCopy.Buffer copyRegion=VkBufferCopy.calloc(1)){
			copyRegion.get(0)
			          .srcOffset(srcOffset)
			          .dstOffset(destOffset)
			          .size(size);
			vkCmdCopyBuffer(cmd, src.getHandle(), getHandle(), copyRegion);
		}
	}
	
}
