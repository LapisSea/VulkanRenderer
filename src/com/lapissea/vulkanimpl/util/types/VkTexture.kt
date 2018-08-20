package com.lapissea.vulkanimpl.util.types

import com.lapissea.vulkanimpl.Vk
import com.lapissea.vulkanimpl.VkGpu
import com.lapissea.vulkanimpl.util.DevelopmentInfo.*
import com.lapissea.vulkanimpl.util.VkConstruct
import com.lapissea.vulkanimpl.util.VkDestroyable
import com.lapissea.vulkanimpl.util.VkGpuCtx
import com.lapissea.vulkanimpl.util.VkImageAspect
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorImageInfo

class VkTexture(val image: VkImage, private val memory: VkDeviceMemory?): VkDestroyable, VkGpuCtx {
	var view: Long=0
		private set
	private var sampler: Long=0
	
	fun createView(aspect: VkImageAspect): VkTexture {
		if(view!=VK_NULL_HANDLE) return this
		
		val lb=memAllocLong(1)
		try {
			VkConstruct.imageViewCreateInfo().use {info->
				info.image(image.handle)
					.viewType(VK_IMAGE_VIEW_TYPE_2D)
					.format(image.format.handle)
				
				info.subresourceRange().set(aspect.`val`, 0, 1, 0, 1)
				info.components().set(VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY)
				
				view=Vk.createImageView(gpu, info, lb)
			}
		} finally {
			memFree(lb)
		}
		return this
	}
	
	fun createSampler(): VkTexture {
		val lb=memAllocLong(1)
		try {
			VkConstruct.samplerCreateInfo().use {samplerInfo->
				samplerInfo.magFilter(VK_FILTER_LINEAR)
					.minFilter(VK_FILTER_LINEAR)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.anisotropyEnable(true)
					.maxAnisotropy(16f)
					.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
					.unnormalizedCoordinates(false)
					.compareEnable(false)
					.compareOp(VK_COMPARE_OP_ALWAYS)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
					.mipLodBias(0f)
					.minLod(0f)
					.maxLod(0f)
				
				sampler=Vk.createSampler(device, samplerInfo, lb)
			}
		} finally {
			memFree(lb)
		}
		
		return this
	}
	
	override fun destroy() {
		if(view!=0L) vkDestroyImageView(device, view, null)
		if(sampler!=0L) vkDestroySampler(device, sampler, null)
		image.destroy()
		memory?.destroy()
	}
	
	override fun getGpu(): VkGpu {
		return image.gpu
	}
	
	fun put(info: VkDescriptorImageInfo.Buffer, pos: Int): VkDescriptorImageInfo.Buffer {
		if(DEV_ON) {
			if(view==0L) throw RuntimeException("Missing view")
			if(sampler==0L) throw RuntimeException("Missing sampler")
		}
		
		info.get(pos)
			.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
			.imageView(view)
			.sampler(sampler)
		return info;
	}
	
}
