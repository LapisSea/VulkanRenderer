package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.DataManager;
import com.lapissea.datamanager.IDataManager;
import com.lapissea.glfw.GlfwMonitor;
import com.lapissea.glfw.GlfwWindow;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.glfw.GLFWImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.IntStream;

public class ApplicationVk{
	
	private static final boolean TO_JAR=true;
	
	private final VulkanRenderer vkRenderer =new VulkanRenderer();
	private final GlfwWindowVk   gameWindow =new GlfwWindowVk();
	private final File           winSaveFile=new File("WindowState.json");
	
	private DataManager  manger;
	private IDataManager textures;
	
	public ApplicationVk(){
		init();
		while(run()) ;
		destroy();
	}
	
	public void init(){
		
		manger=new DataManager();
		try{
			File root=new File(ApplicationVk.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			manger.registerDomain(root);
		}catch(URISyntaxException e){}
		
		textures=manger.subData("assets/textures");
		
		GlfwMonitor.init();
		
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-thread"));
		gameWindow.loadState(winSaveFile);
		gameWindow.title.set("Vulkan attempt 2");
		gameWindow.init();
		
		TIntList pows=new TIntArrayList(5);
		IntStream.range(3, 8).map(i->1<<i).forEach(pows::add);
		
		gameWindow.setIcon(
			textures.getDirNamesS("icon")
			        .parallel()
			        .map(fileName->{
				        try{
					        String name=fileName.substring(fileName.lastIndexOf(File.separatorChar)+1, fileName.lastIndexOf('.'));
					        int    res =Integer.parseInt(name);
					
					        if(pows.contains(res)){
						        try(InputStream i=textures.getInStream("icon/"+fileName)){
							        BufferedImage bi=ImageIO.read(i);
							        if(bi.getWidth()!=res||bi.getHeight()!=res) return null;
							        return bi;
						        }
					        }
				        }catch(Exception e){}
				        return null;
			        })
			        .filter(Objects::nonNull)
			        .map(GlfwWindow::imgToGlfw)
			        .toArray(GLFWImage[]::new));
		
		
		vkRenderer.createContext(gameWindow);
		gameWindow.show();
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
		gameWindow.saveState(winSaveFile);
		vkRenderer.destroy();
		gameWindow.destroy();
	}
	
	private void shutdown(){
	
	}
	
}
