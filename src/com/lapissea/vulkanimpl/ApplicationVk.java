package com.lapissea.vulkanimpl;

import com.lapissea.glfwwin.GlfwMonitor;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;

import java.io.File;

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
		GlfwMonitor.init();
		
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-thread"));
		gameWindow.loadState(winSaveFile);
		gameWindow.title.set("Vulkan attempt 2");
		gameWindow.monitor.set(GlfwMonitor.getMonitors().get(1));
		gameWindow.init()
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
		gameWindow.saveState(winSaveFile);
	}
	
}
