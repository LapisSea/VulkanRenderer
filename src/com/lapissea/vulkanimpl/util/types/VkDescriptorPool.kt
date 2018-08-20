package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.Vk
import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.VkConstruct
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import org.lwjgl.vulkan.VK10.*
import java.nio.LongBuffer

class VkDescriptorPool(private val gpu: VkGpu, val handle: Long): VkDestroyable, VkGpuCtx {
	
	fun allocateDescriptorSets(layouts: VkDescriptorSetLayout)=allocateDescriptorSets(layouts.buff)[0]
	
	fun allocateDescriptorSets(layouts: LongBuffer): Array<VkDescriptorSet> {
		VkConstruct.descriptorSetAllocateInfo().use {
			
			it.descriptorPool(handle).pSetLayouts(layouts)
			
			return Vk.allocateDescriptorSets(gpu, it)
		}
	}
	
	override fun destroy()=vkDestroyDescriptorPool(device, handle, null)
	
	override fun getGpu()=gpu
	
}
