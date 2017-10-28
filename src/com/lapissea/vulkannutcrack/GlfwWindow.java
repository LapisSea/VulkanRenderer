package com.lapissea.vulkannutcrack;

import com.lapissea.vec.Vec2i;
import com.lapissea.vec.interf.IVec2iR;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class GlfwWindow{
	
	private long id;
	
	private String title;
	private Vec2i size=new Vec2i();
	private Vec2i pos =new Vec2i();
	private boolean           resizeable;
	private long              surface;
	private Consumer<IVec2iR> onResize;
	
	public GlfwWindow(){
		setTitle("<NO_NAME>");
		setSize(600, 400);
		setResizeable(false);
	}
	
	public void init(){
		IVec2iR s=getSize();
		glfwWindowHint(GLFW_RESIZABLE, resizeable?GLFW_TRUE:GLFW_FALSE);
		id=glfwCreateWindow(s.x(), s.y(), getTitle(), MemoryUtil.NULL, MemoryUtil.NULL);
		glfwSetWindowPos(id, pos.x(), pos.y());
		
		glfwSetWindowSizeCallback(id, (window, w, h)->{
			if(window!=id) return;
			size.set(w, h);
			if(onResize!=null) onResize.accept(size);
		});
		glfwSetWindowPosCallback(id, (window, xpos, ypos)->{
			if(window!=id) return;
			pos.set(xpos, ypos);
		});
	}
	
	public GlfwWindow setResizeable(boolean resizeable){
		if(!isCreated()) this.resizeable=resizeable;
		return this;
	}
	
	public GlfwWindow setUserPointer(long pointer){
		glfwSetWindowUserPointer(id, pointer);
		return this;
	}
	
	public long getUserPointer(){
		return glfwGetWindowUserPointer(id);
	}
	
	public boolean isResizeable(){
		return resizeable;
	}
	
	public boolean isCreated(){
		return id>0;
	}
	
	public IVec2iR getSize(){
		return size;
	}
	
	public IVec2iR getPos(){
		return pos;
	}
	
	public String getTitle(){
		return title;
	}
	
	public GlfwWindow setTitle(String title){
		this.title=Objects.requireNonNull(title);
		return this;
	}
	
	public GlfwWindow setSize(int width, int height){
		size.set(width, height);
		if(isCreated()) glfwSetWindowSize(id, width, height);
		return this;
	}
	
	public GlfwWindow setPos(int x, int y){
		pos.set(x, y);
		if(isCreated()) glfwSetWindowPos(id, x, y);
		return this;
	}
	
	public boolean shouldClose(){
		return glfwWindowShouldClose(id);
	}
	
	public void destroySurface(VkInstance instance){
		KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
	}
	
	public void destroy(){
		glfwDestroyWindow(id);
		id=0;
	}
	
	
	public void createSurface(VkInstance instance, LongBuffer lp){
		surface=Glfw.createWindowSurface(instance, id, lp);
		
	}
	
	public long getSurface(){
		return surface;
	}
	
	public void onResize(Runnable onResize){
		this.onResize=s->onResize.run();
	}
	
	public void onResize(Consumer<IVec2iR> onResize){
		this.onResize=onResize;
	}
}
