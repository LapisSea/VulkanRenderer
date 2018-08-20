package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.VkBufferedHandle
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import com.lapissea.vulkanimpl.util.format.VkDescriptor
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10
import java.nio.LongBuffer

class VkDescriptorSetLayout(private val buff: LongBuffer, private val gpu: VkGpu, private val parts: Array<VkDescriptor>): VkDestroyable, VkGpuCtx, VkBufferedHandle {
	
	override fun destroy() {
		memFree(buff)
		VK10.vkDestroyDescriptorSetLayout(device, handle, null)
	}
	
	override fun getGpu()=gpu
	override fun getBuff()=buff
	fun getParts()=parts
}
