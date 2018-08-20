package com.lapissea.vulkanimpl.assets;

import com.lapissea.datamanager.DataSignature;
import com.lapissea.util.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.lapissea.vulkanimpl.assets.ICacheableResource.Status.*;

public class ResourceManager<L, T extends ICacheableResource<L>>{
	
	private final List<T> data=new ArrayList<>();
	
	private final Function<DataSignature, T> factory;
	
	public ResourceManager(Function<DataSignature, T> factory){
		this.factory=factory;
	}
	
	public L getNow(DataSignature path){
		T n=getNode(path);
		
		synchronized(n){
			var s=n.getStatus();
			
			//if never tried to load
			if(s==OK&&n.getValue()==null) n.run();
			else if(s.done) n.update();
		}
		
		n.block();
		return n.getValue();
	}
	
	public CompletableFuture<L> get(DataSignature path){
		CompletableFuture<L> onDone;
		
		T n=getNode(path);
		
		synchronized(n){
			var s=n.getStatus();
			
			onDone=new CompletableFuture<>();
			
			//if never tried to load
			if(s==OK&&n.getValue()==null) n.run();
			else if(s.done){
				//if done check for change
				if(n.update()){
					//add listener and run
					n.onload.add(onDone::complete);
					n.run();
				}
				//no change and is loaded so can just get value
				else onDone.complete(n.getValue());
			}
			//is loading so add listener
			else n.onload.add(onDone::complete);
			
			return onDone;
		}
	}
	
	@NotNull
	private T getNode(DataSignature path){
		
		synchronized(data){
			for(T t : data){
				if(t.getSource().equals(path)) return t;
			}
		}
		
		T t=factory.apply(path);
		
		synchronized(data){ data.add(t); }
		
		return t;
	}
	
}
