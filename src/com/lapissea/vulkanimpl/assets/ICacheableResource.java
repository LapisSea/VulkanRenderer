package com.lapissea.vulkanimpl.assets;

import com.lapissea.datamanager.DataSignature;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Consumer;

import static com.lapissea.util.PoolOwnThread.*;
import static com.lapissea.util.UtilL.sleep;
import static com.lapissea.vulkanimpl.assets.ICacheableResource.Status.*;

public abstract class ICacheableResource<T>{
	
	public enum Status{
		STARTING(true, false),
		LOADING_SRC(true, false),
		LOADING_CACHE(true, false),
		MAKING_CACHE(true, false),
		ERROR(false, true),
		MISSING(false, true),
		OK(true, true);
		
		public final boolean ok;
		public final boolean done;
		
		Status(boolean ok, boolean done){
			this.ok=ok;
			this.done=done;
		}
		
		public boolean ready(){
			return ok&&done;
		}
	}
	
	private       Status            status=OK;
	private       Throwable         error;
	private final DataSignature     source;
	private       T                 value;
	public final  List<Consumer<T>> onload=new ArrayList<>();
	
	public ICacheableResource(DataSignature source){
		this.source=source;
	}
	
	public Status getStatus(){
		return status;
	}
	
	private void setStatus(Status status){
		this.status=status;
	}
	
	public Throwable getError(){
		return error;
	}
	
	public T getValue(){
		return getStatus().ok?value:null;
	}
	
	public DataSignature getSource(){
		return source;
	}
	
	public void run(){
		async(()->{
			if(!getStatus().done) throw new ConcurrentModificationException();
			
			value=load();
			synchronized(this){
				if(!onload.isEmpty()){
					onload.forEach(c->c.accept(value));
					onload.clear();
				}
			}
		});
	}
	
	protected T load(){
		setStatus(STARTING);
		try{
			var     cache=getCacheSource(source);
			boolean e1   =source.exists(), e2=cache.exists();
			
			if(!e1){
				if(!e2||!allowHeadless()){
					setStatus(MISSING);
					return null;
				}
				//headless cache
				setStatus(LOADING_CACHE);
				return loadCache(cache);
			}
			
			if(cache.olderThan(source)){
				setStatus(LOADING_SRC);
				T data=loadSource(source);
				setStatus(MAKING_CACHE);
				makeCache(cache, data);
				return data;
			}
			
			setStatus(LOADING_CACHE);
			return loadCache(cache);
		}catch(Throwable e){
			error=e;
			setStatus(ERROR);
			return null;
		}
	}
	
	protected abstract T loadSource(DataSignature source);
	
	protected abstract DataSignature getCacheSource(DataSignature source);
	
	protected abstract T loadCache(DataSignature cacheSource);
	
	protected abstract void makeCache(DataSignature cacheSource, T data);
	
	protected abstract boolean observe();
	
	protected boolean allowHeadless(){return true;}
	
	/**
	 * returns if change was detected
	 */
	public boolean update(){
		if(observe()){
			run();
			return true;
		}
		return false;
	}
	
	public void block(){
		while(!getStatus().done) sleep(1);
	}
}
