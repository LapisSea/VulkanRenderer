package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.Vk
import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.VkConstruct
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer

class VkCommandPool(private val gpu: VkGpu, val handle: Long): VkDestroyable, VkGpuCtx {
	
	fun allocateCommandBuffer(): VkCommandBufferM {
		return allocateCommandBuffers(1)[0]
	}
	
	fun allocateCommandBuffers(count: Int, primary: VkCommandBufferM): Array<VkCommandBufferM> {
		if(!primary.isPrimary) throw RuntimeException("Can't make secondary buffer from a secondary buffer!")
		return allocateCommandBuffers(count, VK_COMMAND_BUFFER_LEVEL_SECONDARY)
	}
	
	fun allocateCommandBuffers(count: Int): Array<VkCommandBufferM> {
		return allocateCommandBuffers(count, VK_COMMAND_BUFFER_LEVEL_PRIMARY)
	}
	
	private fun allocateCommandBuffers(count: Int, level: Int): Array<VkCommandBufferM> {
		stackPush().use {stack->
			val allocInfo=VkConstruct.commandBufferAllocateInfo(stack)
			allocInfo.commandPool(handle)
				.level(level)
				.commandBufferCount(count)
			return Vk.allocateCommandBuffers(getGpu(), this, allocInfo, stack.mallocPointer(count))
		}
	}
	
	fun freeCmd(commandBuffer: VkCommandBuffer) {
		vkFreeCommandBuffers(device, handle, commandBuffer)
	}
	
	fun reset()=VK10.vkResetCommandPool(device, handle, 0)
	
	override fun destroy() {
		vkDestroyCommandPool(device, handle, null)
	}
	
	override fun getGpu(): VkGpu {
		return gpu
	}
	
}
