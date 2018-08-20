package com.lapissea.vulkanimpl.util;

import com.lapissea.glfw.GlfwWindow;
import com.lapissea.vulkanimpl.VulkanCore;
import com.lapissea.vulkanimpl.util.types.VkSurface;

import static org.lwjgl.glfw.GLFWVulkan.*;

public class GlfwWindowVk extends GlfwWindow{
	
	public VkSurface createSurface(VulkanCore renderer){
		long[] result={0};
		int    code  =glfwCreateWindowSurface(renderer.getInstance(), handle, null, result);
		return new VkSurface(result[0], renderer);
	}
}
