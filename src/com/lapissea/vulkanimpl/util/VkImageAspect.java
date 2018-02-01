package com.lapissea.vulkanimpl.util;

import java.util.regex.Pattern;

import static org.lwjgl.vulkan.VK10.*;

public enum VkImageAspect{
	
	COLOR(VK_IMAGE_ASPECT_COLOR_BIT, "R[0-9]+"),
	DEPTH(VK_IMAGE_ASPECT_DEPTH_BIT, "D[0-9]+"),
	STENCIL(VK_IMAGE_ASPECT_STENCIL_BIT, "S[0-9]+"),
	METADATA(VK_IMAGE_ASPECT_METADATA_BIT, "M[0-9]+");
	
	public final int     val;
	public final Pattern detectionPattern;
	
	VkImageAspect(int val, String pattern){
		this.val=val;
		detectionPattern=Pattern.compile(pattern);
	}
}
