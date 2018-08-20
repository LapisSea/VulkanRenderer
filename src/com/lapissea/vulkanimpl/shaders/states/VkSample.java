package com.lapissea.vulkanimpl.shaders.states;

import static org.lwjgl.vulkan.VK10.*;

public enum VkSample{
	
	SAMPLE1(VK_SAMPLE_COUNT_1_BIT),
	SAMPLE2(VK_SAMPLE_COUNT_2_BIT),
	SAMPLE3(VK_SAMPLE_COUNT_4_BIT),
	SAMPLE4(VK_SAMPLE_COUNT_8_BIT),
	SAMPLE5(VK_SAMPLE_COUNT_16_BIT),
	SAMPLE6(VK_SAMPLE_COUNT_32_BIT),
	SAMPLE7(VK_SAMPLE_COUNT_64_BIT);
	
	public final int handle;
	
	VkSample(int handle){
		this.handle=handle;
	}
}
