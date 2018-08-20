package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.SingleUseCommandBuffer
import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.DevelopmentInfo.*
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkMemoryRequirements
import java.nio.LongBuffer

class VkBuffer(gpuCtx: VkGpuCtx, val buff: LongBuffer, val size: Long): VkGpuCtx, VkDestroyable {
	
	private val gpu: VkGpu=gpuCtx.gpu
	
	val handle: Long
		get()=buff.get(0)
	
	override fun destroy() {
		VK10.vkDestroyBuffer(device, buff.get(0), null)
		memFree(buff)
	}
	
	override fun getGpu(): VkGpu=gpu
	
	
	fun createMemory(requestedProperties: Int): VkDeviceMemory {
		VkMemoryRequirements.malloc().use {memRequirements->
			val mem=getGpu().allocateMemory(gpu.getMemRequirements(memRequirements, this), size, requestedProperties)
			mem.bindBuffer(this)
			return mem
		}
	}
	
	fun copyFrom(src: VkBuffer, srcOffset: Long, transferPool: VkCommandPool) {
		copyFrom(src, srcOffset, 0, size, transferPool)
	}
	
	fun copyFrom(src: VkBuffer, srcOffset: Long, destOffset: Long, size: Long, transferPool: VkCommandPool) {
		SingleUseCommandBuffer(gpu.transferQueue, transferPool).use {cmd-> copyFrom(src, srcOffset, destOffset, size, cmd.commandBuffer)}
	}
	
	fun copyFrom(src: VkBuffer, srcOffset: Long, cmd: VkCommandBuffer) {
		copyFrom(src, srcOffset, 0, size, cmd)
	}
	
	fun copyFrom(src: VkBuffer, srcOffset: Long, destOffset: Long, size: Long, cmd: VkCommandBuffer) {
		
		if(DEV_ON) {
			if(srcOffset<0) throw IndexOutOfBoundsException("Source offset can't be negative!")
			if(destOffset<0) throw IndexOutOfBoundsException("Destination offset can't be negative!")
			if(size>this.size) throw IndexOutOfBoundsException("Size has to be less or equal to destination size!")
			if(size>src.size) throw IndexOutOfBoundsException("Size has to be less or equal to source size!")
			if(srcOffset+size>this.size) throw IndexOutOfBoundsException("Destination range of "+srcOffset+" - "+(srcOffset+size)+" is out of range!")
			if(srcOffset+size>src.size) throw IndexOutOfBoundsException("Source range of "+srcOffset+" - "+(srcOffset+size)+" is out of range!")
		}
		
		VkBufferCopy.calloc(1).use {copyRegion->
			copyRegion.get(0)
				.srcOffset(srcOffset)
				.dstOffset(destOffset)
				.size(size)
			vkCmdCopyBuffer(cmd, src.handle, handle, copyRegion)
		}
	}
	
}
