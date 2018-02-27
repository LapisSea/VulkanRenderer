package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import org.lwjgl.vulkan.VkExtent2D;

import java.util.HashMap;
import java.util.Map;

public class LaunchVk{
	
	
	public static void main(String[] args){
		LogUtil.__.create(true, false, "log");
		
		Map<String, String> parsedArgs=new HashMap<>();
		
		for(String arg : args){
			int split=arg.indexOf('=');
			if(split!=-1) parsedArgs.put(arg.substring(0, split), arg.substring(split+1));
			else parsedArgs.put(arg, "");
		}
		
		sysProps(parsedArgs);
		
		TextUtil.__REGISTER_CUSTOM_TO_STRING(VkExtent2D.class, extent2D->extent2D.getClass().getSimpleName()+"{width="+extent2D.width()+", height="+extent2D.height()+"}");
		
		new ApplicationVk();
	}
	
	private static void sysProps(Map<String, String> parsedArgs){
//		System.setProperty("joml.nounsafe", "true");
//		System.setProperty("joml.fastmath", "true");
//		System.setProperty("joml.sinLookup", "true");
		if(parsedArgs.containsKey("dev"))System.setProperty("VulkanRenderer.devMode", parsedArgs.get("dev"));
	}
	
	
}
