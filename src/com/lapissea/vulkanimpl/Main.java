package com.lapissea.vulkanimpl;

import com.lapissea.util.ArgumentParser;
import com.lapissea.util.LogUtil;

import static com.lapissea.util.LogUtil.Init.*;

public class Main{
	
	static{ LogUtil.Init.attach(USE_CALL_THREAD|USE_CALL_POS|USE_TABULATED_HEADER, "log"); }
	
	public static void main(String[] args) throws Exception{ main(new ArgumentParser(args));}
	
	public static void main(ArgumentParser args) throws Exception{
		
		ClassLoader vcl=ClassLoader.getSystemClassLoader();
//		vcl=new VulkanClassLoader(vcl);
		
		try{
			sysProps(args);
			vcl.loadClass("com.lapissea.vulkanimpl.ApplicationVk").newInstance();
//			new ApplicationVk();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	private static void sysProps(ArgumentParser parsedArgs){
//		System.setProperty("joml.nounsafe", "true");
//		System.setProperty("joml.fastmath", "true");
//		System.setProperty("joml.sinLookup", "true");
		System.setProperty("VulkanCore.devMode", parsedArgs.getString("dev", ""));
	}
	
}
