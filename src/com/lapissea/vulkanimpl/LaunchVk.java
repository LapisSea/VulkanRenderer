package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;

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
		
		new ApplicationVk();
	}
	
	private static void sysProps(Map<String, String> parsedArgs){
		System.setProperty("VulkanRenderer.devMode", parsedArgs.get("dev"));
	}
	
	
}
