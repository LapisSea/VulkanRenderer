package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.DataManager;
import com.lapissea.glfw.GlfwKeyboardEvent;
import com.lapissea.glfw.GlfwMonitor;
import com.lapissea.glfw.GlfwWindow;
import com.lapissea.util.LogUtil;
import com.lapissea.util.NanoTimer;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.FpsCounter;
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

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.glfw.GLFW.*;

public class ApplicationVk{
	
	private static final boolean TO_JAR=true;
	
	private VulkanRenderer vkRenderer;
	private GlfwWindowVk gameWindow =new GlfwWindowVk();
	private File         winSaveFile=new File("WindowState.json");
	
	private DataManager manger;
	
	private FpsCounter fps=new FpsCounter(true);
	private long lastFpsPrint;
	
	public ApplicationVk(){
		init();
		while(run()) ;
		destroy();
	}
	
	public void init(){
		fps.activate();
		
		manger=new DataManager();
		if(DEV_ON){
			manger.registerDomain(new File("res"));
		}else{
			try{
				File root=new File(ApplicationVk.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				manger.registerDomain(root);
			}catch(URISyntaxException e){
				UtilL.uncheckedThrow(e);
			}
		}
		
		vkRenderer=new VulkanRenderer(manger);
//		vkRenderer.getSettings().trippleBufferingEnabled.set(false);
		GlfwMonitor.init();
		
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-thread"));
		
		setUpWindow();
		
		NanoTimer timer=new NanoTimer();
		timer.start();
		vkRenderer.createContext(gameWindow);
		timer.end();
		
		gameWindow.registryKeyboardKey.register(event->{
			if(event.type!=GlfwKeyboardEvent.Type.DOWN) return;
			switch(event.key){
			case GLFW_KEY_F11:{
				if(gameWindow.monitor.get()!=null) gameWindow.monitor.set(null);
				else gameWindow.setAutoFullScreen();
				break;
			}
			case GLFW_KEY_ESCAPE:{
				gameWindow.requestClose();
				break;
			}
			}
		});
		
		gameWindow.show();
		LogUtil.println("Engine initialized in", timer.s(), "seconds");
	}
	
	private void setUpWindow(){
		gameWindow.loadState(winSaveFile);
		gameWindow.title.set("Vulkan attempt 2");
		gameWindow.init();
		
		TIntList pows=new TIntArrayList(5);
		IntStream.range(3, 8).map(i->1<<i).forEach(pows::add);
		
		String iconsPath="assets/textures/icon/";
		gameWindow.setIcon(
			manger.getDirNamesS(iconsPath)
			      .parallel()
			      .map(fileName->{
				      try{
					      String name=fileName.substring(fileName.lastIndexOf(File.separatorChar)+1, fileName.lastIndexOf('.'));
					      int    res =Integer.parseInt(name);
					
					      if(pows.contains(res)){
						      try(InputStream i=manger.getInStream(iconsPath+fileName)){
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
		
	}
	
	public boolean run(){
		gameWindow.pollEvents();
		if(gameWindow.shouldClose()) return false;
		render();
		return true;
	}
	
	private void render(){
		if(gameWindow.isHidden()){
			UtilL.sleep(1);
			return;
		}
		
		vkRenderer.render();
		fps.newFrame();
		
		long tim=System.currentTimeMillis();
		if(lastFpsPrint+1000<tim){
			lastFpsPrint=tim;
			LogUtil.println(fps);
		}
	}
	
	public void destroy(){
		gameWindow.saveState(winSaveFile);
		gameWindow.hide();
		vkRenderer.destroy();
		gameWindow.destroy();
	}
	
	private void shutdown(){
	
	}
	
}
