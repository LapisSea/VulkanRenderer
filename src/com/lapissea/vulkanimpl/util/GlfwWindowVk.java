package com.lapissea.vulkanimpl.util;

import com.lapissea.glfw.GlfwWindow;
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
	
	public long createSurface(VkInstance instance){
		long[] result={0};
		int    code  =glfwCreateWindowSurface(instance, handle, null, result);
		return result[0];
	}
}
