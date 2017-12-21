package com.lapissea.vulkanimpl.util;

import static org.lwjgl.vulkan.VK10.*;

public enum VkImageAspect{
	
	COLOR(VK_IMAGE_ASPECT_COLOR_BIT),
	DEPTH(VK_IMAGE_ASPECT_DEPTH_BIT),
	STENCIL(VK_IMAGE_ASPECT_STENCIL_BIT),
	METADATA(VK_IMAGE_ASPECT_METADATA_BIT);
	
	public final int val;
	
	VkImageAspect(int val){
		this.val=val;
	}
}
