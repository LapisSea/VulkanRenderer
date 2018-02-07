package com.lapissea.vulkanimpl.util;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

import java.util.function.LongSupplier;

public class FpsCounter{
	
	private TLongList frames=new TLongArrayList(30);
	private       boolean active;
	private final int     second;
	
	private final LongSupplier tim;
	
	public FpsCounter(boolean useNano){
		if(useNano){
			second=1000_000_000;
			tim=System::nanoTime;
		}else{
			second=1000;
			tim=System::currentTimeMillis;
		}
	}
	
	public void newFrame(){
		if(!active) return;
		
		long time=tim.getAsLong();
		
		TLongIterator i=frames.iterator();
		while(i.hasNext()){
			if(time-i.next()>second) i.remove();
		}
		frames.add(time);
	}
	
	public int getFps(){
		return frames.size();
	}
	
	public void activate(){
		active=true;
	}
	
	public void deactivate(){
		active=false;
		frames.clear();
	}
	
	@Override
	public String toString(){
		return "FPS: "+getFps();
	}
	
}