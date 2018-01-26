package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;

public class LaunchVk{
	
	
	public static void main(String[] args){
		sysProps();
		
		LogUtil.__.create(true,false,"log");
		
		new ApplicationVk();
	}
	
	private static void sysProps(){
		System.setProperty("VulkanRenderer.devMode","true");
		
		
	}
	
	
	
}
