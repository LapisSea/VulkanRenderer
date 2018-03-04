package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkTexture implements VkDestroyable, VkGpuCtx{
	
	private final VkImage        image;
	private       long           view;
	private       VkDeviceMemory memory;
	private       long           sampler;
	
	public VkTexture(VkImage image, VkDeviceMemory memory){
		this.image=image;
		this.memory=memory;
	}
	
	public VkTexture createView(VkImageAspect aspect){
		if(view!=VK_NULL_HANDLE) return this;
		
		LongBuffer lb=memAllocLong(1);
		try(VkImageViewCreateInfo info=VkConstruct.imageViewCreateInfo()){
			info.image(image.getHandle())
			    .viewType(VK_IMAGE_VIEW_TYPE_2D)
			    .format(image.getFormat().handle);
			
			info.subresourceRange().set(aspect.val, 0, 1, 0, 1);
			info.components().set(VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY);
			
			view=Vk.createImageView(getGpu(), info, lb);
		}finally{
			memFree(lb);
		}
		return this;
	}
	
	public VkTexture createSampler(){
		LongBuffer lb=memAllocLong(1);
		try(VkSamplerCreateInfo samplerInfo=VkConstruct.samplerCreateInfo()){
			samplerInfo.magFilter(VK_FILTER_LINEAR)
			           .minFilter(VK_FILTER_LINEAR)
			           .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .anisotropyEnable(true)
			           .maxAnisotropy(16)
			           .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
			           .unnormalizedCoordinates(false)
			           .compareEnable(false)
			           .compareOp(VK_COMPARE_OP_ALWAYS)
			           .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
			           .mipLodBias(0)
			           .minLod(0)
			           .maxLod(0);
			
			sampler=Vk.createSampler(getDevice(), samplerInfo, lb);
		}finally{
			memFree(lb);
		}
		
		return this;
	}
	
	public long getView(){
		return view;
	}
	
	@Override
	public void destroy(){
		if(view!=0) vkDestroyImageView(getDevice(), view, null);
		if(sampler!=0) vkDestroySampler(getDevice(), sampler, null);
		image.destroy();
		if(memory!=null) memory.destroy();
	}
	
	@Override
	public VkGpu getGpu(){
		return image.getGpu();
	}
	
	public VkImage getImage(){
		return image;
	}
	
	public void put(VkDescriptorImageInfo info){
		if(DEV_ON){
			if(view==0) throw new RuntimeException("Missing view");
			if(sampler==0) throw new RuntimeException("Missing sampler");
		}
		
		info.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		    .imageView(view)
		    .sampler(sampler);
	}
}
