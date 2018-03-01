package com.lapissea.vulkanimpl.util;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;

public class DevelopmentInfo{
	
	public static final boolean DEV_ON;
	
	static{
		String key="VulkanCore.devMode",
			devArg0=System.getProperty(key, "false"),
			devArg=devArg0.toLowerCase();
		
		if(devArg.equals("true")) DEV_ON=true;
		else{
			if(devArg.equals("false")) DEV_ON=false;
			else throw UtilL.exitWithErrorMsg("Invalid property: "+key+"="+devArg0+" (valid: \"true\", \"false\", \"\")");
		}
		System.setProperty("org.lwjgl.util.NoChecks", Boolean.toString(DEV_ON));
		LogUtil.println("Running VulkanCore in "+(DEV_ON?"development":"production")+" mode");
	}
	
	public static void checkOnLoad(){
		if(DEV_ON) return;
		for(StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()){
			if(stackTraceElement.getMethodName().equals("<clinit>")){
				throw UtilL.uncheckedThrow(new IllegalAccessException("Class "+stackTraceElement.getClassName()+" is development only"));
			}
		}
		throw UtilL.uncheckedThrow(new IllegalAccessException("Unknown class is development only"));
	}
}
