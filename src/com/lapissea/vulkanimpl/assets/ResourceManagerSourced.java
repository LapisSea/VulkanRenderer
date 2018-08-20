package com.lapissea.vulkanimpl.assets;

import com.lapissea.datamanager.DataSignature;
import com.lapissea.datamanager.IDataManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ResourceManagerSourced<L, T extends ICacheableResource<L>> extends ResourceManager<L, T>{
	
	public final IDataManager source;
	
	public ResourceManagerSourced(IDataManager source, Function<DataSignature, T> factory){
		super(factory);
		this.source=source;
	}
	
	public L getNow(String path){
		return getNow(source.createSignature(path));
	}
	
	public CompletableFuture<L> get(String path){
		return get(source.createSignature(path));
	}
}
