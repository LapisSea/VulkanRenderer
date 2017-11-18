package com.lapissea.vulkanimpl;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

public class FpsCounter{
	
	private TLongList frames=new TLongArrayList(30);
	private       boolean active;
	private final int     second;
	private final boolean useNano;
	
	public FpsCounter(boolean useNano){
		this.useNano=useNano;
		second=useNano?1000_000_000:1000;
	}
	
	public void newFrame(){
		if(!active) return;
		
		long time=useNano?System.nanoTime():System.currentTimeMillis();
		
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