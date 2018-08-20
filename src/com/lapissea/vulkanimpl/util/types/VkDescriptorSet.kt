package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.VkBufferedHandle
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import com.lapissea.vulkanimpl.util.format.VkDescriptor
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCopyDescriptorSet
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.nio.LongBuffer

class VkDescriptorSet(private val gpu: VkGpu, private val buff:LongBuffer): VkDestroyable, VkGpuCtx, VkBufferedHandle {
	
	override fun getBuff()=buff
	override fun getGpu()=gpu
	
	override fun destroy() {
		buff.put(0,0)
		memFree(buff)
	}
	
	
	fun update(descriptors: Array<VkDescriptor>) {
		MemoryStack.stackPush().use {
			return update(VkDescriptor.createWriteDescriptorSet(descriptors, this, it))
		}
	}
	fun update(descriptorWrites: VkWriteDescriptorSet.Buffer) {
		update(descriptorWrites, null)
	}
	
	fun update(descriptorWrites: VkWriteDescriptorSet.Buffer, descriptorCopies: VkCopyDescriptorSet.Buffer?) {
		vkUpdateDescriptorSets(device, descriptorWrites, descriptorCopies)
	}
}
