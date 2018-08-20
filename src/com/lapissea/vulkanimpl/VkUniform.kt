package com.lapissea.vulkanimpl

import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.types.VkBuffer
import com.lapissea.vulkanimpl.util.types.VkDeviceMemory
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import java.nio.ByteBuffer
import java.util.function.*

class VkUniform(gpu: VkGpu, size: Int, private val writeBufferData: Consumer<ByteBuffer>): VkDestroyable {
	
	private val buffer: VkBuffer=gpu.createBuffer(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, size.toLong())
	private val memory: VkDeviceMemory=buffer.createMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
	
	fun updateBuffer()=memory.memorySession(buffer.size, writeBufferData)
	
	override fun destroy() {
		buffer.destroy()
		memory.destroy()
	}
	
	fun put(info: VkDescriptorBufferInfo.Buffer, pos: Int): VkDescriptorBufferInfo.Buffer {
		
		info.get(pos)
			.buffer(buffer.handle)
			.offset(0)
			.range(buffer.size)
		
		return info;
	}
}
