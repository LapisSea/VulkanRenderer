package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.Vk
import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VkCommandBufferM(handle: Long, private val pool: VkCommandPool, val isPrimary: Boolean): VkCommandBuffer(handle, pool.gpu.device), VkGpuCtx, VkDestroyable {
	
	fun createSecondary(count: Int): Array<VkCommandBufferM> {
		return pool.allocateCommandBuffers(count, this)
	}
	
	override fun getGpu(): VkGpu {
		return pool.gpu
	}
	
	fun begin() {
		stackPush().use {stack->
			
			val beginInfo=VkCommandBufferBeginInfo.callocStack(stack)
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
				.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
			
			if(!isPrimary) {
				val inheritanceInfo=VkCommandBufferInheritanceInfo.callocStack(stack)
				inheritanceInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
				beginInfo.pInheritanceInfo(inheritanceInfo)
			}
			
			Vk.beginCommandBuffer(this, beginInfo)
		}
	}
	
	fun endRenderPass()=vkCmdEndRenderPass(this)
	
	
	fun end()=Vk.endCommandBuffer(this)
	
	fun reset()=Vk.resetCommandBuffer(this, 0);
	
	override fun destroy()=vkFreeCommandBuffers(device, pool.handle, this)
	
	
	fun pipelineBarrier(srcStageMask: Int, dstStageMask: Int, dependencyFlags: Int, pImageMemoryBarriers: VkImageMemoryBarrier.Buffer)=pipelineBarrier(srcStageMask, dstStageMask, dependencyFlags, null, null, pImageMemoryBarriers)
	
	fun pipelineBarrier(srcStageMask: Int, dstStageMask: Int, dependencyFlags: Int, pMemoryBarriers: VkMemoryBarrier.Buffer?, pBufferMemoryBarriers: VkBufferMemoryBarrier.Buffer?, pImageMemoryBarriers: VkImageMemoryBarrier.Buffer)=vkCmdPipelineBarrier(this, srcStageMask, dstStageMask, dependencyFlags, pMemoryBarriers, pBufferMemoryBarriers, pImageMemoryBarriers)
	
	
	fun copyBufferToImage(buffer: VkBuffer, image: VkImage, region: VkBufferImageCopy.Buffer)=vkCmdCopyBufferToImage(this, buffer.handle, image.handle, image.layout, region)
	
	override fun getDevice(): VkDevice {
		return super<VkGpuCtx>.getDevice()
	}
}
