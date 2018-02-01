package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkTexture implements VkDestroyable, VkGpuCtx{
	
	private final VkImage image;
	private long view=VK_NULL_HANDLE;
	
	public VkTexture(VkImage image){
		this.image=image;
	}
	
	public VkTexture createView(int format, VkImageAspect aspect){
		if(view!=VK_NULL_HANDLE) return this;
		
		try(MemoryStack stack=stackPush()){
			
			VkImageViewCreateInfo info=VkImageViewCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
			    .image(image.getHandle())
			    .viewType(VK_IMAGE_VIEW_TYPE_2D)
			    .format(format);
			
			info.subresourceRange().set(aspect.val, 0, 1, 0, 1);
			
			info.components().set(VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY);
			
			view=Vk.createImageView(getGpu(), info, stack.mallocLong(1));
		}
		return this;
	}
	
	public long getView(){
		return view;
	}
	
	@Override
	public void destroy(){
		if(view!=VK_NULL_HANDLE) vkDestroyImageView(getGpu().getDevice(), view, null);
		image.destroy();
		
	}
	
	@Override
	public VkGpu getGpu(){
		return image.getGpu();
	}
}
