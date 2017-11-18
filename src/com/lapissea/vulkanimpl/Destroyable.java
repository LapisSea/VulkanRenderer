package com.lapissea.vulkanimpl;

import java.util.ArrayList;
import java.util.List;

public class Destroyable{
	
	private       List<Destroyable> children;
	private final Runnable          hook;
	
	public Destroyable(Runnable hook){
		this.hook=hook;
	}
	
	public void addChild(Runnable d){
		addChild(new Destroyable(d));
	}
	
	public void addChild(Destroyable d){
		if(children==null) children=new ArrayList<>();
		children.add(d);
	}
	
	public void destroy(){
		if(children!=null){
			children.forEach(Destroyable::destroy);
			children.clear();
		}
		
		hook.run();
	}
}
