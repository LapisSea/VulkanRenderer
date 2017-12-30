package com.lapissea.vulkanimpl.util;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.Vk;

import java.lang.reflect.Field;

public class DevelopmentMemorySafety{
	
	static{
		if(!Vk.DEVELOPMENT) throw new UnsupportedOperationException();
	}
	
	public static StackTraceElement[] make(){
		return Thread.currentThread().getStackTrace();
	}
	
	public static void check(Object subject, Object... things){
		for(Object thing : things){
			if(thing!=null) kill(subject);
		}
	}
	
	private static void kill(Object subject){
		String stackName="_ctorStack", name=subject.getClass().getName();
		try{
			Field ctorStackF=subject.getClass().getDeclaredField(stackName);
			ctorStackF.setAccessible(true);
			StackTraceElement[] ctorStack=(StackTraceElement[])ctorStackF.get(subject);
			
			LogUtil.printlnEr(name+" not cleaned up! This is a memory leak! \nConstructed at:");
			for(StackTraceElement s : ctorStack){
				LogUtil.printlnEr(s);
			}
		}catch(NoSuchFieldException e){
			LogUtil.printlnEr("No "+stackName+" field in "+name);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
