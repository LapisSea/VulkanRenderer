package com.lapissea.vulkanimpl;

import com.lapissea.util.PairM;
import com.lapissea.vec.Vec2i;
import com.lapissea.vec.interf.IVec2iR;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkInstance;

import java.awt.geom.Rectangle2D;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class GlfwWindow{
	
	private long id;
	
	private String title;
	private Vec2i size=new Vec2i();
	private Vec2i pos =new Vec2i(-1, -1);
	private boolean           resizeable;
	private boolean           maximized;
	private long              surface;
	private Consumer<IVec2iR> onResize;
	
	private static final List<Rectangle2D> DISPLAYS          =new ArrayList<>(1);
	private static final List<Rectangle2D> DISPLAY_GROUPS    =new ArrayList<>(1);
	private static       long              LAST_DISPLAY_FETCH=0;
	
	public GlfwWindow(){
		setTitle("<NO_NAME>");
		setSize(600, 400);
		setResizeable(false);
	}
	
	public void init(){
		IVec2iR s=getSize();
		glfwWindowHint(GLFW_RESIZABLE, resizeable?GLFW_TRUE:GLFW_FALSE);
//		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		id=glfwCreateWindow(s.x(), s.y(), getTitle(), MemoryUtil.NULL, MemoryUtil.NULL);
		setWindowPos(pos);
		
		glfwSetWindowSizeCallback(id, (window, w, h)->{
			if(window!=id) return;
			size.set(w, h);
			if(onResize!=null) onResize.accept(size);
		});
		glfwSetWindowPosCallback(id, (window, xpos, ypos)->{
			if(window!=id) return;
			pos.set(xpos, ypos);
		});
		glfwSetWindowMaximizeCallback(id, (window, maximized)->{
			if(window!=id) return;
			this.maximized=maximized;
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
	
	
	public void setWindowPos(IVec2iR pos){
		setWindowPos(pos.x(), pos.y());
	}
	
	public synchronized void setWindowPos(int x, int y){
		if(x==-1&&y==-1){
			GLFWVidMode mode=glfwGetVideoMode(glfwGetPrimaryMonitor());
			
			x=(mode.width()-getSize().x())/2;
			y=(mode.height()-getSize().y())/2;
		}
		
		long tim=System.currentTimeMillis();
		if(tim-LAST_DISPLAY_FETCH>1000*30){
			LAST_DISPLAY_FETCH=tim;
			
			DISPLAYS.clear();
			DISPLAY_GROUPS.clear();
			
			try(MemoryStack stack=MemoryStack.stackPush()){
				PointerBuffer monitors=glfwGetMonitors();
				
				IntBuffer xBuf=stack.callocInt(1);
				IntBuffer yBuf=stack.callocInt(1);
				
				for(int i=0;i<monitors.capacity();i++){
					long monitor=monitors.get(i);
					
					glfwGetMonitorPos(monitor, xBuf, yBuf);
					GLFWVidMode mode=glfwGetVideoMode(monitor);
					
					DISPLAYS.add(new Rectangle2D.Float(xBuf.get(0), yBuf.get(0), mode.width(), mode.height()));
				}
			}
			List<Rectangle2D> all=new ArrayList<>(1);
			for(Rectangle2D d : DISPLAYS){
				all.addAll(DISPLAY_GROUPS);
				all.addAll(DISPLAYS);
				
				Rectangle2D group=new Rectangle2D.Float((float)d.getX(), (float)d.getY(), (float)d.getWidth(), (float)d.getHeight());
				
				boolean change=true;
				while(change){
					change=false;
					
					for(Rectangle2D display : all){
						if(display.equals(group)) continue;
						
						boolean yFlush=group.getMaxY()==display.getMaxY()&&group.getMinY()==display.getMinY();
						if(yFlush){
							boolean rightCon=group.getMaxX()==display.getMinX();
							boolean leftCon =display.getMaxX()==group.getMinX();
							if(rightCon||leftCon){
								change=true;
								group=group.createUnion(display);
								
							}
						}
						
						boolean xFlush=group.getMaxX()==display.getMaxX()&&group.getMinX()==display.getMinX();
						if(xFlush){
							boolean topCon   =group.getMaxY()==display.getMinY();
							boolean bottomCon=display.getMaxY()==group.getMinY();
							if(topCon||bottomCon){
								change=true;
								group=group.createUnion(display);
							}
						}
						
					}
				}
				
				if(!DISPLAY_GROUPS.contains(group)) DISPLAY_GROUPS.add(group);
			}
		}
		
		Rectangle2D window=new Rectangle2D.Float(x, y, getSize().x(), getSize().y());
		
		if(DISPLAY_GROUPS.stream().noneMatch(group->group.contains(window))){
			DISPLAY_GROUPS.stream()
			              .map(group->new PairM<>(group, group.createIntersection(new Rectangle2D.Float((float)group.getX(), (float)group.getY(), getSize().x(), getSize().y()))))
			              .max(Comparator.comparingDouble(a->a.obj2.getWidth()*a.obj2.getHeight()))
			              .ifPresent(best->{
				              setSize((int)best.obj2.getWidth(), (int)best.obj2.getHeight());
				              setWindowPos(((int)best.obj1.getWidth()-getSize().x())/2, ((int)best.obj1.getHeight()-getSize().y())/2);
			              });
			return;
		}
		
		glfwSetWindowPos(id, x, y);
	}
	
	
	public void createSurface(VkInstance instance){
		try(MemoryStack stack=MemoryStack.stackPush()){
			surface=Glfw.createWindowSurface(instance, id, stack.mallocLong(1));
		}
	}
	
	public boolean isMaximized(){
		return maximized;
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
	
	public void hide(){
		glfwHideWindow(id);
	}
	
	public void show(){
		glfwShowWindow(id);
	}
}
