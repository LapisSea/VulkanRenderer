package com.lapissea.vulkanimpl;

import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;

import java.io.File;
import java.util.stream.Stream;

public class ApplicationVk{
	
	private final VulkanRenderer vkRenderer =new VulkanRenderer();
	private final GlfwWindowVk   gameWindow =new GlfwWindowVk();
	private final File           winSaveFile=new File("WindowState.json");
	
	public ApplicationVk(){
		init();
		while(run()) ;
		destroy();
	}
	
	public void init(){
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-thread"));
		
		gameWindow.loadState(winSaveFile)
		          .setTitle("Vulkan attempt 2")
		          .setResizeable(true)
		          .init()
		          .show();
		
		vkRenderer.createContext(gameWindow);
	}
	
	public boolean run(){
		gameWindow.pollEvents();
		if(gameWindow.shouldClose()) return false;
		
		vkRenderer.render();
		
		UtilL.sleep(1);
		return true;
	}
	
	
	public void destroy(){
		gameWindow.hide();
		gameWindow.destroy();
	}
	
	private void shutdown(){
		Stream.of(
			(Runnable)()->gameWindow.loadState(winSaveFile),
			()->{
			
			}).parallel().forEach(Runnable::run);
	}
	
}
