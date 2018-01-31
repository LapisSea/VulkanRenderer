package com.lapissea.vulkanimpl.util;

import com.lapissea.glfw.GlfwWindow;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.vulkan.VkInstance;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;

public class GlfwWindowVk extends GlfwWindow{
	
	
	public GlfwWindowVk(){
	
	}
	
	@Override
	public GlfwWindow init(){
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		return super.init();
	}
	
	public VkSurface createSurface(VkInstance instance){
		long[] result={0};
		int    code  =glfwCreateWindowSurface(instance, handle, null, result);
		return new VkSurface(result[0]);
	}
}
