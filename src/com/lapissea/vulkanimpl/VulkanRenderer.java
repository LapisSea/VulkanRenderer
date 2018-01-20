package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkInstance;

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
		VK10.VK_QUEUE_FAMILY_EXTERNAL_KHR
	}
	
	public void createContext(GlfwWindowVk window){
		this.window=window;
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
