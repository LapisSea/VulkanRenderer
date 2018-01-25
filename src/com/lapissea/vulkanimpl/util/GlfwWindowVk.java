package com.lapissea.vulkanimpl.util;

import com.lapissea.glfwwin.GlfwWindow;
import com.lapissea.vulkanimpl.VulkanRenderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class GlfwWindowVk extends GlfwWindow{
	
	private long surface;
	
	public GlfwWindowVk(){
	
	}
	
	@Override
	public GlfwWindowVk init(){
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		super.init();
		return this;
	}
	
	public void destroySurface(VulkanRenderer vk){
		vkDestroySurfaceKHR(vk.getInstance(), surface, null);
	}
	
	public void createSurface(VulkanRenderer vk){
		long[] result={0};
		int    code  =glfwCreateWindowSurface(vk.getInstance(), handle, null, result);
		surface=result[0];
	}
	
	public long getSurface(){
		return surface;
	}
}
