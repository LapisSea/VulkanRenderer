package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.util.List;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements VkDestroyable{
	
	public static final boolean DEVELOPMENT;
	
	static{
		String key="VulkanRenderer.devMode",
			devArg0=System.getProperty(key, "false"),
			devArg=devArg0.toLowerCase();
		
		if(devArg.equals("true")) DEVELOPMENT=true;
		else{
			if(devArg.equals("false")) DEVELOPMENT=false;
			else throw UtilL.exitWithErrorMsg("Invalid property: "+key+"="+devArg0+" (valid: \"true\", \"false\", \"\")");
		}
		
		LogUtil.println("Running VulkanRenderer in "+(DEVELOPMENT?"development":"production")+" mode");
	}
	
	
	private VkInstance instance;
	
	private VkGpu renderGpu, computeGpu;
	
	private GlfwWindowVk window;
	
	public VulkanRenderer(){
	
	}
	
	public void render(){
	
	}
	
	public void createContext(GlfwWindowVk window){
		this.window=window;
		try(MemoryStack stack=stackPush()){
			
			PointerBuffer
				layers=Vk.stringsToPP(List.of(), stack),
				extensions=Vk.stringsToPP(List.of(), stack);
			
			VkInstanceCreateInfo info=VkInstanceCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
			    .pApplicationInfo(Vk.initAppInfo(stack, window.getTitle(), "0.0.1", "JLapisor", "0.0.1"))
			    .ppEnabledLayerNames(layers)
			    .ppEnabledExtensionNames(extensions);
			
			instance=Vk.createInstance(info);
		}
	}
	
	public VkInstance getInstance(){
		return instance;
	}
	
	public VkGpu getRenderGpu(){
		return renderGpu;
	}
	
	public VkGpu getComputeGpu(){
		return computeGpu;
	}
	
	@Override
	public void destroy(){
	
	}
	
}
