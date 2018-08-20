package com.lapissea.vulkanimpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lapissea.datamanager.managers.DataManagerSingle;
import com.lapissea.glfw.GlfwMonitor;
import com.lapissea.glfw.GlfwWindow;
import com.lapissea.util.LogUtil;
import com.lapissea.util.NanoTimer;
import com.lapissea.util.PoolOwnThread;
import com.lapissea.util.UtilL;
import com.lapissea.vec.Vec2f;
import com.lapissea.vec.Vec2i;
import com.lapissea.vulkanimpl.exceptions.VkException;
import com.lapissea.vulkanimpl.util.FpsCounter;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.lapissea.glfw.GlfwKeyboardEvent.Type.*;
import static com.lapissea.glfw.GlfwWindow.Cursor.*;
import static com.lapissea.glfw.GlfwWindow.SurfaceAPI.*;
import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.glfw.GLFW.*;

public class ApplicationVk{
	
	private static final boolean TO_JAR=true;
	
	private VulkanCore   vkRenderer;
	private GlfwWindowVk gameWindow;
	private File         winSaveFile =new File("WindowState.json");
	private File         gameSaveFile=new File("game.json");
	
	private DataManagerSingle manger;
	
	private FpsCounter fps=new FpsCounter(true);
	private long       lastFpsPrint, lastUpdate;
	
	public ApplicationVk(){
		LogUtil.println(ApplicationVk.class.getClassLoader());
		if(!VulkanCore.VULKAN_INSTALLED){
			FailMsg.create("Vulkan driver not installed/compatible! :(");
			return;
		}
		gameWindow=new GlfwWindowVk();
		init();
		var v=CompletableFuture.allOf(
			gameWindow.whileOpen(()->{
				try{
					UtilL.sleep(1);
					updateKeys();
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}, "Update"),
			gameWindow.whileOpen(()->{
				try{
					UtilL.sleep(1);
					render();
				}catch(Exception e){
					e.printStackTrace();
					System.exit(0);
				}
			}, "vk", PoolOwnThread::new));
		
		gameWindow.whileOpen(()->{
			try{
				UtilL.sleep(1);
				gameWindow.pollEvents();
			}catch(Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		});
		v.join();
		destroy();
		LogUtil.println("asd");
	}
	
	float moveSpeed=2;
	
	private void updateKeys(){
		
		long   tim  =System.nanoTime();
		double delta=(tim-lastUpdate)/1000_000_000D;
		lastUpdate=tim;
		
		Vector3f movement=new Vector3f();
		
		if(gameWindow.isKeyDown(GLFW_KEY_LEFT_SHIFT)) movement.y-=1;
		if(gameWindow.isKeyDown(GLFW_KEY_SPACE)) movement.y+=1;
		if(gameWindow.isKeyDown(GLFW_KEY_A)) movement.x-=1;
		if(gameWindow.isKeyDown(GLFW_KEY_D)) movement.x+=1;
		if(gameWindow.isKeyDown(GLFW_KEY_W)) movement.z-=1;
		if(gameWindow.isKeyDown(GLFW_KEY_S)) movement.z+=1;
		
		if(movement.lengthSquared()>0){
			movement.normalize((float)(delta*moveSpeed*moveSpeed));
			movement.mulTranspose(new Matrix3f().rotateY(vkRenderer.camera.rotation.y()));
			vkRenderer.camera.pos.add(movement.x, movement.y, movement.z);
		}
		
		var r=vkRenderer.camera.rotation;
		if(gameWindow.isKeyDown(GLFW_KEY_Q)) r.subZ(0.01F);
		if(gameWindow.isKeyDown(GLFW_KEY_E)) r.addZ(0.01F);
		if(r.z()!=0){
			r.mulZ(0.98F);
			if(Math.abs(r.z())<0.00001) r.z(0);
		}
	}
	
	private final Vec2f lastPos=new Vec2f();
	
	private void cameraRot(Vec2i pos){
		if(!gameWindow.isFocused()) return;
		
		var mode3d=DISABLED;
//		var mode3d=NORMAL;
		if(gameWindow.cursorMode.get()!=mode3d){
			//ignore last pos when mouse reengtering window
			gameWindow.cursorMode.set(mode3d);
			lastPos.set(pos);
			return;
		}
		
		float sensitivity=200;
		vkRenderer.camera.rotation.addXY(lastPos.sub(pos).flipXY().div(-sensitivity));
		lastPos.set(pos);
		vkRenderer.camera.rotation.clampX(-(float)Math.PI/2, (float)Math.PI/2);
	}
	
	public void init(){
		//when window is focused but not clicked inside frame buffer, a mouse
		// click is necessary to grab mouse when second click is in frame buffer
		gameWindow.registryMouseButton.register(event->cameraRot(gameWindow.mousePos));
		
		gameWindow.mousePos.register(this::cameraRot);
		gameWindow.focused.register(focused->{
			//on unfocused release mouse
			if(!focused) gameWindow.cursorMode.set(NORMAL);
		});
		gameWindow.registryKeyboardKey.register(GLFW_KEY_F11, DOWN, e->gameWindow.toggleFullScreen());
		gameWindow.registryKeyboardKey.register(GLFW_KEY_ESCAPE, DOWN, e->gameWindow.requestClose());
		
		if(DEV_ON) gameWindow.registryKeyboardKey.register(GLFW_KEY_F12, UP, e->renderDoc());
		
		gameWindow.registryMouseScroll.register(scroll->{
			moveSpeed=Math.max(0, moveSpeed+scroll.y()/4);
		});
		
		lastUpdate=System.nanoTime();
		
		
		fps.activate();
		try{
			manger=new DataManagerSingle(LogUtil.printlnAndReturn(DEV_ON?"res":ApplicationVk.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
		}catch(URISyntaxException e){ throw UtilL.uncheckedThrow(e); }
		
		vkRenderer=new VulkanCore(manger.subData("assets"), gameWindow);
//		vkRenderer.getSettings().tripleBufferingEnabled.set(false);
		vkRenderer.getSettings().tripleBufferingEnabled.set(true);
		GlfwMonitor.init();
		
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-thread"));
		
		setUpWindow();
		
		NanoTimer timer=new NanoTimer();
		try{
			timer.start();
			vkRenderer.createContext();
			timer.end();
		}catch(VkException e){
			e.printStackTrace();
			FailMsg.create("It's crash my dudes! AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			return;
		}
		LogUtil.println("Engine initialized in", timer.s(), "seconds");
		
		try(var v=new FileReader(gameSaveFile)){
			var c=new Gson().fromJson(v, VkCamera.class);
			if(c!=null) vkRenderer.camera=c;
		}catch(Exception ignored){}
		
		gameWindow.show();
		gameWindow.focus();
		gameWindow.centerMouse();
	}
	
	private void renderDoc(){
		try{
			Desktop.getDesktop().open(Arrays.stream(new File("C:\\Users\\LapisSea\\AppData\\Local\\Temp\\RenderDoc").listFiles((f, n)->n.endsWith(".rdc"))).max(Comparator.comparingLong(File::lastModified)).get());
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void setUpWindow(){
		gameWindow.loadState(winSaveFile);
		gameWindow.title.set("Vulkan attempt 2");
		gameWindow.init(VULKAN);
		
		PoolOwnThread.async(()->{
			TIntList pows=new TIntArrayList(5);
			IntStream.range(3, 8).map(i->1<<i).forEach(pows::add);
//			STBImage.stbi_load_16()
			gameWindow.setIcon(
				Objects.requireNonNull(manger.getDirPathsS("assets/textures/icon/"))
				       .parallel()
				       .map(path->{
					       String name=path.substring(path.lastIndexOf(File.separatorChar)+1, path.lastIndexOf('.'));
					       int    res =Integer.parseInt(name);
					
					       if(pows.contains(res)){
						       try(InputStream i=manger.getInStream(path)){
							       if(i==null) return null;
							       BufferedImage bi=ImageIO.read(i);
							       if(bi.getWidth()!=res||bi.getHeight()!=res) return null;
							       return bi;
						       }catch(Exception ignored){}
					       }
					       return null;
				       })
				       .filter(Objects::nonNull)
				       .map(GlfwWindow::imgToGlfw)
				       .toArray(GLFWImage[]::new));
		});
	}
	
	private void render(){
		if(!gameWindow.isVisible()){
			UtilL.sleep(1);
			return;
		}
		vkRenderer.render();
		
		fps.newFrame();
//
//		long tim=System.currentTimeMillis();
//		if(lastFpsPrint+250<tim){
//			lastFpsPrint=tim;
//			LogUtil.println(fps);
//		}
	}
	
	public synchronized void destroy(){
		if(vkRenderer==null) return;
		try{
			try(var f=new FileWriter(gameSaveFile)){
				
				new GsonBuilder().setPrettyPrinting()
				                 .disableHtmlEscaping()
				                 .create()
				                 .toJson(vkRenderer.camera, f);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		gameWindow.saveState(winSaveFile);
		gameWindow.hide();
		vkRenderer.destroy();
		gameWindow.destroy();
		vkRenderer=null;
	}
	
	private void shutdown(){
//		destroy();
	}
	
}
