package com.lapissea.vulkannutcrack;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;


public class Glfw{
	
	public static void init(){
		GLFWErrorCallback.createPrint().set();
		if(!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
		if(!glfwVulkanSupported()) throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
	}
	
	public static long createWindowSurface(VkInstance instance, long window, LongBuffer dest){
		int code=GLFWVulkan.glfwCreateWindowSurface(instance, window, null, dest);
		return dest.get(0);
	}
	
}
